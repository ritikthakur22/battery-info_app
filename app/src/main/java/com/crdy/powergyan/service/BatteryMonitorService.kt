package com.crdy.powergyan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.crdy.powergyan.AlarmActivity
import com.crdy.powergyan.MainActivity
import com.crdy.powergyan.data.local.SettingsDataStore
import com.crdy.powergyan.data.local.history.BatteryHistoryHelper
import com.crdy.powergyan.data.local.history.UsageTracker
import com.crdy.powergyan.domain.model.BatterySnapshot
import com.crdy.powergyan.domain.model.ChargeDecision
import com.crdy.powergyan.domain.model.PluggedState
import com.crdy.powergyan.domain.model.SmartChargeConfig
import com.crdy.powergyan.domain.model.decideCharging
import com.crdy.powergyan.platform.access.ChargeSwitch
import com.crdy.powergyan.platform.access.ChargeTarget
import com.crdy.powergyan.platform.alerts.BatteryAlertController
import com.crdy.powergyan.platform.battery.AndroidBatteryDataSource
import com.crdy.powergyan.widget.BatteryGlanceWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class BatteryMonitorService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var alertController: BatteryAlertController
    private lateinit var usageTracker: UsageTracker

    // Smart Charge state for the current plug-in session.
    private var target: ChargeTarget? = null
    private var lastDecision: ChargeDecision? = null
    private var justPlugged = true

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            usageTracker.tick(System.currentTimeMillis(), null, intent.action == Intent.ACTION_SCREEN_ON, null)
        }
    }

    companion object {
        const val ACTION_STOP_ALARM = "com.crdy.powergyan.action.STOP_ALARM"
        private const val CHANNEL_MONITOR = "battery_monitor"
        private const val CHANNEL_ALARM = "battery_alarm_high"
        private const val CHANNEL_LOW = "battery_low"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        if (!runCatching { startForeground(1, createNotification("Starting battery monitor…")) }.isSuccess) {
            stopSelf()
            return
        }

        alertController = BatteryAlertController(applicationContext, onTriggerAlarm = ::showChargeAlarm, onLowBattery = ::showLowBattery)
        usageTracker = UsageTracker(applicationContext)
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
        val settingsStore = SettingsDataStore(applicationContext)
        val historyHelper = BatteryHistoryHelper(applicationContext)
        val powerManager = getSystemService(PowerManager::class.java)

        serviceScope.launch {
            combine(AndroidBatteryDataSource(applicationContext).observeBatteryState(), settingsStore.displaySettings) { snapshot, settings ->
                Triple(snapshot, settings.alertPolicy, settings.smartChargeConfig)
            }.collect { (snapshot, policy, config) ->
                val pluggedIn = snapshot.plugged != PluggedState.NONE
                alertController.onSnapshot(snapshot, policy)
                if (historyHelper.insertSnapshot(snapshot)) runCatching { BatteryGlanceWidget().updateAll(applicationContext) }
                usageTracker.tick(System.currentTimeMillis(), snapshot.percentage, powerManager.isInteractive, pluggedIn)

                val smartStatus = if (config.enabled && pluggedIn && !ChargeSwitch.testInProgress) {
                    withContext(Dispatchers.IO) { enforceSmartCharge(snapshot, config) }
                } else null
                updateNotification(notificationText(snapshot, config, smartStatus))

                // We stay alive while unplugged to catch the next plug-in, but leave charging on whenever we're
                // not actively managing it, so a stopped service or turned-off feature never blocks charging.
                if (!pluggedIn || !config.enabled) {
                    if (!justPlugged) withContext(Dispatchers.IO) { restoreCharging() }
                    justPlugged = true
                }

                if (!policy.enabled && !config.enabled) stopSelf()
            }
        }
    }

    private fun enforceSmartCharge(snapshot: BatterySnapshot, config: SmartChargeConfig): String {
        val t = ChargeSwitch.detect(config) ?: return "Can't find this phone's charging control. Set it in Control."
        target = t
        val now = Calendar.getInstance()
        val (stop, resume) = config.limitsFor(snapshot.plugged, now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE))
        val decision = decideCharging(
            level = snapshot.percentage,
            tempC = snapshot.temperatureC,
            stop = stop,
            resume = resume,
            maxTempC = config.maxTempC.takeIf { config.tempControlEnabled },
            previous = lastDecision,
            justPlugged = justPlugged
        )
        justPlugged = false
        if (!ChargeSwitch.set(t, decision.charge)) return "Couldn't change charging. Check that root access is still granted."
        lastDecision = decision
        return when {
            decision.forHeat -> String.format(Locale.getDefault(), "Paused while the battery cools (%.1f°C)", snapshot.temperatureC ?: 0f)
            !decision.charge -> "Holding at $stop%. Charging starts again at $resume%."
            stop == 100 && config.fullByMinute >= 0 -> "Topping up to 100% for ${formatMinuteOfDay(config.fullByMinute)}"
            else -> "Charging to $stop%"
        }
    }

    private fun restoreCharging() {
        target?.let { ChargeSwitch.set(it, true) }
        lastDecision = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALARM && ::alertController.isInitialized) {
            alertController.stopAlarms()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        restoreCharging()
        if (::alertController.isInitialized) {
            alertController.stopAlarms()
            runCatching { unregisterReceiver(screenReceiver) }
        }
        serviceScope.cancel()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_MONITOR, "Battery monitor", NotificationManager.IMPORTANCE_LOW))
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ALARM, "Charge alarm", NotificationManager.IMPORTANCE_HIGH))
        manager.createNotificationChannel(NotificationChannel(CHANNEL_LOW, "Low battery", NotificationManager.IMPORTANCE_HIGH))
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun createNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_MONITOR)
            .setContentTitle("PowerGyan")
            .setContentText(text)
            .setSmallIcon(com.crdy.powergyan.R.drawable.ic_bolt)
            .setContentIntent(openAppIntent())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(1, createNotification(text))
    }

    private fun notificationText(snapshot: BatterySnapshot, config: SmartChargeConfig, smartStatus: String?): String = when {
        snapshot.plugged == PluggedState.NONE -> "On battery, ${snapshot.percentage}%"
        config.enabled -> smartStatus ?: "Smart Charge is on"
        else -> "Charging, ${snapshot.percentage}%"
    }

    private fun showChargeAlarm() {
        val fullScreen = PendingIntent.getActivity(
            this, 2,
            Intent(this, AlarmActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ALARM)
            .setContentTitle("Charging target reached")
            .setContentText("Unplug your charger to keep the battery healthy.")
            .setSmallIcon(com.crdy.powergyan.R.drawable.ic_bolt)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreen, true)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(2, notification)
    }

    private fun showLowBattery(level: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_LOW)
            .setContentTitle("Battery at $level%")
            .setContentText("Plug in soon.")
            .setSmallIcon(com.crdy.powergyan.R.drawable.ic_bolt)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(3, notification)
    }
}

fun formatMinuteOfDay(minute: Int): String = Calendar.getInstance().run {
    set(Calendar.HOUR_OF_DAY, minute / 60)
    set(Calendar.MINUTE, minute % 60)
    java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(time)
}
