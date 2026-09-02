package com.crdy.powergyan.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.crdy.powergyan.data.local.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_LOCKED_BOOT_COMPLETED || action == Intent.ACTION_POWER_CONNECTED) {
            val settingsStore = SettingsDataStore(context)
            CoroutineScope(Dispatchers.IO).launch {
                val settings = settingsStore.displaySettings.first()
                if (settings.alertPolicy.enabled || settings.smartChargeConfig.enabled) {
                    val serviceIntent = Intent(context, BatteryMonitorService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
        }
    }
}
