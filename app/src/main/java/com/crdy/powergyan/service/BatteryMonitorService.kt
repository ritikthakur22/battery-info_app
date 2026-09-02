package com.crdy.powergyan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.crdy.powergyan.MainActivity
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

    companion object {
        const val ACTION_STOP_ALARM = "com.crdy.powergyan.action.STOP_ALARM"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (!runCatching { startForeground(1, createNotification("Starting battery monitor...")) }.isSuccess) {
            stopSelf()
            return
        }

        alertController = BatteryAlertController(applicationContext) { triggerFullscreenAlarm() }
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

                val isPluggedIn = snapshot.plugged != com.crdy.powergyan.domain.model.PluggedState.NONE
                
                // SMART CHARGE & TEMPERATURE CONTROL LOGIC
                val smartStatus = if (config.enabled && isPluggedIn) {
                    enforceSmartCharge(snapshot.percentage, snapshot.temperatureC ?: 0f, config)
                } else null
                
                updateNotification(notificationText(snapshot, config, smartStatus))

                // We no longer stop the service on unplug! We must stay alive in the background
                // to instantly catch the next plug event. This is required for Android 8.0+.
                if (!isPluggedIn) {
                    isFirstEvaluation = true
                }

                if (!policy.enabled && !config.enabled) {
                    stopSelf()
                }
            }
        }
    }

    private var detectedPath: String? = null
    private var detectedEnable: String = "1"
    private var detectedDisable: String = "0"
    private var lastDisabledByTemp = false
    private var isFirstEvaluation = true

    private fun autoDetectControlFile(config: SmartChargeConfig) {
        if (rootFileExists(config.ctrlPath)) {
            detectedPath = config.ctrlPath
            val isInputSuspend = config.ctrlPath.endsWith("input_suspend") ||
                (config.ctrlPath.endsWith("battery_charging_enabled") && rootFileExists("/sys/class/power_supply/battery/input_suspend"))
            detectedEnable = if (isInputSuspend && config.ctrlEnable == "1" && config.ctrlDisable == "0") "0" else config.ctrlEnable
            detectedDisable = if (isInputSuspend && config.ctrlEnable == "1" && config.ctrlDisable == "0") "1" else config.ctrlDisable
            
            if (config.ctrlPath.endsWith("battery_charging_enabled") && rootFileExists("/sys/class/power_supply/battery/input_suspend")) {
                detectedPath = "/sys/class/power_supply/battery/input_suspend"
            }
            return
        }
        
        // Massive Auto-Detect Database (BCL Inspired)
        val universalPaths = listOf(
            Triple("/sys/class/power_supply/battery/battery_charging_enabled", "1", "0"),
            Triple("/sys/class/power_supply/battery/charging_enabled", "1", "0"),
            Triple("/sys/class/power_supply/battery/input_suspend", "0", "1"),
            Triple("/sys/class/power_supply/battery/store_mode", "0", "1"),
            Triple("/sys/class/power_supply/wireless/store_mode", "0", "1"),
            Triple("/sys/class/power_supply/mains/store_mode", "0", "1"),
            Triple("/sys/class/power_supply/battery/charge_control_limit", "1", "0"),
            Triple("/sys/class/power_supply/battery/charge_control_limit_max", "1", "0")
        )
        
        for (item in universalPaths) {
            if (rootFileExists(item.first)) {
                detectedPath = item.first
                detectedEnable = item.second
                detectedDisable = item.third
                return
            }
        }
        
        detectedPath = config.ctrlPath // Fallback
        detectedEnable = config.ctrlEnable
        detectedDisable = config.ctrlDisable
    }

    private suspend fun enforceSmartCharge(level: Int, tempC: Float, config: SmartChargeConfig): String = withContext(Dispatchers.IO) {
            autoDetectControlFile(config)
            val path = detectedPath ?: config.ctrlPath
            val enableValue = detectedEnable
            val disableValue = detectedDisable
            
            val shouldStopForTemp = config.tempControlEnabled && tempC >= config.maxTempC
            
            val desiredValue = when {
                shouldStopForTemp -> {
                    lastDisabledByTemp = true
                    disableValue
                }
                level >= config.stopLimit -> {
                    lastDisabledByTemp = false
                    disableValue
                }
                level <= config.resumeLimit -> {
                    lastDisabledByTemp = false
                    enableValue
                }
                else -> {
                    if (isFirstEvaluation) {
                        // User physically re-plugged the charger while between limits. Force charge!
                        lastDisabledByTemp = false
                        enableValue
                    } else if (lastDisabledByTemp) {
                        lastDisabledByTemp = false
                        enableValue
                    } else {
                        readControlValue(path) ?: enableValue
                    }
                }
            }
            
            isFirstEvaluation = false
            
            val written = writeControlValue(path, desiredValue)
            if (written) {
                if (desiredValue == disableValue) {
                    if (shouldStopForTemp) "Paused (Cooling down) • ${tempC}°C"
                    else "Maintaining charge • ${config.resumeLimit}%–${config.stopLimit}%"
                } else "Charging • resumes at ${config.resumeLimit}% and stops at ${config.stopLimit}%"
            } else "Charge control unavailable • check control file"
    }

    private fun writeControlValue(path: String, value: String): Boolean {
        if (!path.matches(Regex("/sys/[A-Za-z0-9_./,-]+")) ||
            !value.matches(Regex("[A-Za-z0-9_.-]+"))) return false
            
        if (readControlValue(path) == value) return true
        
        val command = "printf '%s' '$value' > '$path'"
        return runCatching {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true).start()
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                false
            } else process.exitValue() == 0
        }.getOrDefault(false)
    }

    private fun rootFileExists(path: String): Boolean {
        if (File(path).exists()) return true
        return runCatching {
            val process = ProcessBuilder("su", "-c", "test -e '$path'").start()
            process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0
        }.getOrDefault(false)
    }

    private fun readControlValue(path: String): String? {
        if (!path.matches(Regex("/sys/[A-Za-z0-9_./,-]+"))) return null
        
        // Fast path: Try standard read without root (Sysfs is usually readable)
        runCatching {
            val text = File(path).readText().trim()
            if (text.isNotEmpty()) return text
        }
        
        // Fallback: Read with Root for restricted devices
        return runCatching {
            val process = ProcessBuilder("su", "-c", "cat '$path'").start()
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            if (process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0) output else null
        }.getOrNull()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALARM) {
            if (::alertController.isInitialized) {
                alertController.stopAlarms()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (::alertController.isInitialized) {
            alertController.stopAlarms()
        }
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
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        

        return NotificationCompat.Builder(this, "battery_monitor")
            .setContentTitle("PowerGyan Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, createNotification(text))
    }

    private fun notificationText(snapshot: com.crdy.powergyan.domain.model.BatterySnapshot, config: SmartChargeConfig, smartStatus: String?): String {
        val isPluggedIn = snapshot.plugged != com.crdy.powergyan.domain.model.PluggedState.NONE
        if (!isPluggedIn) return "Monitoring • Ready for charger"
        return if (config.enabled) smartStatus ?: "Active"
        else "Alerts active • Battery ${snapshot.percentage}%"
    }
    private fun triggerFullscreenAlarm() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "battery_alarm_high",
                "Battery Alarms (High Priority)",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val intent = android.content.Intent(this, com.crdy.powergyan.AlarmActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = android.app.PendingIntent.getActivity(
            this, 2, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        

        val notification = androidx.core.app.NotificationCompat.Builder(this, "battery_alarm_high")
            .setContentTitle("Charge Target Reached!")
            .setContentText("Your battery has reached the target limit.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            
            .setAutoCancel(true)
            .build()
            
        manager.notify(2, notification)
    }

}