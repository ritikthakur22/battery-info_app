package com.crdy.batterygyan.platform.alerts

import android.content.Context
import android.media.RingtoneManager
import com.crdy.batterygyan.domain.model.AlertPolicy
import com.crdy.batterygyan.domain.model.BatterySnapshot

/** Foreground-safe, edge-triggered built-in alerts. It never loops or polls. */
class BatteryAlertController(private val context: Context) {
    private val prefs = context.getSharedPreferences("alert_edges", Context.MODE_PRIVATE)

    fun onSnapshot(snapshot: BatterySnapshot, policy: AlertPolicy) {
        if (!policy.enabled) return
        val crossedLow = snapshot.percentage <= policy.safeThreshold && !prefs.getBoolean("low_latched", false)
        val crossedFull = snapshot.percentage >= 100 && !prefs.getBoolean("full_latched", false)
        if (crossedLow) {
            play()
            prefs.edit().putBoolean("low_latched", true).apply()
        }
        if (crossedFull) {
            play()
            prefs.edit().putBoolean("full_latched", true).apply()
        }
        if (snapshot.percentage > policy.safeThreshold) prefs.edit().putBoolean("low_latched", false).apply()
        if (snapshot.percentage < 100) prefs.edit().putBoolean("full_latched", false).apply()
    }

    private fun play() {
        RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))?.play()
    }
}
