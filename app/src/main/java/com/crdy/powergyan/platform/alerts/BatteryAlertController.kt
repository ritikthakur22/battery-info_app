package com.crdy.powergyan.platform.alerts

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import com.crdy.powergyan.domain.model.AlertPolicy
import com.crdy.powergyan.domain.model.BatterySnapshot

/** Foreground-safe, edge-triggered built-in alerts. It never loops or polls. */
class BatteryAlertController(private val context: Context) {
    enum class PreviewState { PLAYING, PAUSED, STOPPED }
    private val prefs = context.getSharedPreferences("alert_edges", Context.MODE_PRIVATE)

    fun onSnapshot(snapshot: BatterySnapshot, policy: AlertPolicy) {
        if (!policy.enabled) return
        val previousPlugged = prefs.getString("plugged_state", null)
        val currentPlugged = snapshot.plugged.name
        if (previousPlugged != null && previousPlugged != currentPlugged) {
            if (snapshot.plugged == com.crdy.powergyan.domain.model.PluggedState.NONE) {
                play(policy.unplugSoundUri)
            } else if (previousPlugged == com.crdy.powergyan.domain.model.PluggedState.NONE.name) {
                play(policy.plugSoundUri)
            }
        }
        prefs.edit().putString("plugged_state", currentPlugged).apply()
        val previousLevel = prefs.getInt("battery_level", -1)
        val crossedTarget = snapshot.status == com.crdy.powergyan.domain.model.BatteryStatus.CHARGING &&
            previousLevel in 0 until policy.safeThreshold && snapshot.percentage >= policy.safeThreshold &&
            !prefs.getBoolean("target_latched", false)
        val crossedLow = snapshot.status != com.crdy.powergyan.domain.model.BatteryStatus.CHARGING &&
            snapshot.percentage <= policy.safeThreshold && !prefs.getBoolean("low_latched", false)
        val crossedFull = snapshot.percentage >= 100 && !prefs.getBoolean("full_latched", false)
        if (crossedLow) {
            play(policy.customSoundUri)
            prefs.edit().putBoolean("low_latched", true).apply()
        }
        if (crossedTarget) {
            play(policy.customSoundUri)
            prefs.edit().putBoolean("target_latched", true).apply()
        }
        if (crossedFull) {
            play(policy.customSoundUri)
            prefs.edit().putBoolean("full_latched", true).apply()
        }
        if (snapshot.percentage > policy.safeThreshold) prefs.edit().putBoolean("low_latched", false).apply()
        if (snapshot.percentage < policy.safeThreshold) prefs.edit().putBoolean("target_latched", false).apply()
        if (snapshot.percentage < 100) prefs.edit().putBoolean("full_latched", false).apply()
        prefs.edit().putInt("battery_level", snapshot.percentage).apply()
    }

    private fun play(customUri: String?) {
        try {
            if (customUri != null) {
                val uri = android.net.Uri.parse(customUri)
                val mediaPlayer = android.media.MediaPlayer().apply {
                    setDataSource(context, uri)
                    setAudioAttributes(android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                    prepare()
                }
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener { it.release() }
            } else {
                RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) 
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))?.play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to default
            RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))?.play()
        }
    }

    companion object {
        private var preview: MediaPlayer? = null
        private var previewUri: String? = null

        fun playPreview(context: Context, uriString: String?): PreviewState {
            val uri = uriString?.let { android.net.Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (previewUri == uriString && preview != null) {
                if (preview?.isPlaying == true) {
                    preview?.pause()
                    return PreviewState.PAUSED
                }
                runCatching { preview?.start() }.onFailure { preview = null }
                return if (preview != null) PreviewState.PLAYING else PreviewState.STOPPED
            }
            preview?.release()
            preview = null
            previewUri = uriString
            return runCatching {
                MediaPlayer.create(context, uri)?.apply {
                    setAudioAttributes(android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                    setOnCompletionListener {
                        it.release()
                        preview = null
                        previewUri = null
                    }
                    start()
                }.also { preview = it }
                if (preview != null) PreviewState.PLAYING else PreviewState.STOPPED
            }.getOrElse {
                preview = null
                previewUri = null
                PreviewState.STOPPED
            }
        }
    }
}
