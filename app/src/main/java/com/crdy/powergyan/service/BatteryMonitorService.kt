package com.crdy.powergyan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.crdy.powergyan.data.local.SettingsDataStore
import com.crdy.powergyan.domain.model.SmartChargeConfig
import com.crdy.powergyan.platform.alerts.BatteryAlertController
import com.crdy.powergyan.platform.battery.AndroidBatteryDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class BatteryMonitorService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var alertController: BatteryAlertController
    private lateinit var batteryDataSource: AndroidBatteryDataSource
    private lateinit var settingsStore: SettingsDataStore

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (!runCatching { startForeground(1, createNotification("Starting battery monitor...")) }.isSuccess) {
            stopSelf()
            return
        }

        alertController = BatteryAlertController(applicationContext)
        settingsStore = SettingsDataStore(applicationContext)
        batteryDataSource = AndroidBatteryDataSource(applicationContext, null)
        val historyHelper = com.crdy.powergyan.data.local.history.BatteryHistoryHelper(applicationContext)
        
        var lastLevel = -1
        var lastCharging = false

        serviceScope.launch {
            combine(
                batteryDataSource.observeBatteryState(),
                settingsStore.displaySettings
            ) { snapshot, settings ->
                Triple(snapshot, settings.alertPolicy, settings.smartChargeConfig)
            }.collect { (snapshot, policy, config) ->
                alertController.onSnapshot(snapshot, policy)
                val isCharging = snapshot.status == com.crdy.powergyan.domain.model.BatteryStatus.CHARGING
                if (snapshot.percentage != lastLevel || isCharging != lastCharging) {
                    historyHelper.insertSnapshot(snapshot)
                    lastLevel = snapshot.percentage
                    lastCharging = isCharging
                }

                // SMART CHARGE & TEMPERATURE CONTROL LOGIC
                val smartStatus = if (config.enabled) {
                    enforceSmartCharge(snapshot.percentage, snapshot.temperatureC ?: 0f, config)
                } else null
                updateNotification(notificationText(snapshot.percentage, config, smartStatus))

                if (!policy.enabled && !config.enabled) {
                    stopSelf()
                }
            }
        }
    }

    private suspend fun enforceSmartCharge(level: Int, tempC: Float, config: SmartChargeConfig): String = withContext(Dispatchers.IO) {
            val shouldStopForTemp = config.tempControlEnabled && tempC >= config.maxTempC
            val inputSuspend = config.ctrlPath.endsWith("/input_suspend") ||
                (config.ctrlPath.endsWith("/battery_charging_enabled") &&
                    rootFileExists("/sys/class/power_supply/battery/input_suspend"))
            val enableValue = if (inputSuspend && config.ctrlEnable == "1" && config.ctrlDisable == "0") "0" else config.ctrlEnable
            val disableValue = if (inputSuspend && config.ctrlEnable == "1" && config.ctrlDisable == "0") "1" else config.ctrlDisable
            
            val desiredValue = when {
                shouldStopForTemp || level >= config.stopLimit -> disableValue
                level <= config.resumeLimit -> enableValue
                else -> readControlValue(config.ctrlPath) ?: enableValue
            }
            val written = writeControlValue(config.ctrlPath, desiredValue)
            if (written) {
                if (desiredValue == disableValue) "Maintaining charge • ${config.resumeLimit}%–${config.stopLimit}%"
                else "Charging • resumes at ${config.resumeLimit}% and stops at ${config.stopLimit}%"
            } else "Charge control unavailable • check control file"
    }

    private fun writeControlValue(path: String, value: String): Boolean {
        val actualPath = if (path == "/sys/class/power_supply/battery/battery_charging_enabled" &&
            rootFileExists("/sys/class/power_supply/battery/input_suspend")) {
            "/sys/class/power_supply/battery/input_suspend"
        } else path
        if (!actualPath.matches(Regex("/sys/[A-Za-z0-9_./,-]+")) ||
            !value.matches(Regex("[A-Za-z0-9_.-]+"))) return false
        val command = "test -e '$actualPath' && test -w '$actualPath' && printf '%s' '$value' > '$actualPath' && test \"\$(cat '$actualPath')\" = '$value'"
        return runCatching {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true).start()
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                false
            } else process.exitValue() == 0
        }.getOrDefault(false)
    }

    private fun rootFileExists(path: String): Boolean = runCatching {
        val process = ProcessBuilder("su", "-c", "test -e '$path'").start()
        process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0
    }.getOrDefault(false)

    private fun readControlValue(path: String): String? {
        val actualPath = if (path == "/sys/class/power_supply/battery/battery_charging_enabled" &&
            rootFileExists("/sys/class/power_supply/battery/input_suspend")) {
            "/sys/class/power_supply/battery/input_suspend"
        } else path
        if (!actualPath.matches(Regex("/sys/[A-Za-z0-9_./,-]+"))) return null
        return runCatching {
            val process = ProcessBuilder("su", "-c", "cat '$actualPath'").start()
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            if (process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0) output else null
        }.getOrNull()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "battery_monitor",
                "Battery Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, "battery_monitor")
            .setContentTitle("PowerGyan Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, createNotification(text))
    }

    private fun notificationText(level: Int, config: SmartChargeConfig, smartStatus: String?): String {
        return if (config.enabled) "PowerGyan limit • $level% • $smartStatus"
        else "PowerGyan alerts active • Battery $level%"
    }
}
