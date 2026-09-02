package com.crdy.powergyan.platform.alerts

import android.content.Context
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import com.crdy.powergyan.domain.model.AlertPolicy
import com.crdy.powergyan.domain.model.BatterySnapshot

/** Foreground-safe, edge-triggered built-in alerts. */
class BatteryAlertController(private val context: Context, private val onTriggerAlarm: (() -> Unit)? = null) {
    enum class PreviewState { PLAYING, PAUSED, STOPPED }
    private val prefs = context.getSharedPreferences("alert_edges", Context.MODE_PRIVATE)

    private var activeMediaPlayer: MediaPlayer? = null
    private var activeRingtone: Ringtone? = null
    private var memoryPluggedState: String? = null
    private var lastCableEventTime = 0L

    fun stopAlarms() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.cancel(2)
        runCatching { activeMediaPlayer?.stop() }
        runCatching { activeMediaPlayer?.release() }
        activeMediaPlayer = null

        runCatching { activeRingtone?.stop() }
        activeRingtone = null
    }

    fun onSnapshot(snapshot: BatterySnapshot, policy: AlertPolicy) {
        if (!policy.enabled) {
            stopAlarms()
            return
        }
        val previousPlugged = memoryPluggedState ?: prefs.getString("plugged_state", null)
        val currentPlugged = snapshot.plugged.name
        
        // Stop looping alarm if user unplugs device
        if (currentPlugged == com.crdy.powergyan.domain.model.PluggedState.NONE.name && previousPlugged != currentPlugged) {
            stopAlarms()
        }

        val currentTime = System.currentTimeMillis()
        if (previousPlugged != null && previousPlugged != currentPlugged && currentTime - lastCableEventTime > 2000) {
            lastCableEventTime = currentTime
            if (snapshot.plugged == com.crdy.powergyan.domain.model.PluggedState.NONE) {
                play(policy.unplugSoundUri, loop = false, isAlarm = false)
            } else if (previousPlugged == com.crdy.powergyan.domain.model.PluggedState.NONE.name) {
                play(policy.plugSoundUri, loop = false, isAlarm = false)
            }
        }
        memoryPluggedState = currentPlugged
        prefs.edit().putString("plugged_state", currentPlugged).apply()
        
        val previousLevel = prefs.getInt("battery_level", -1)
        val crossedTarget = snapshot.status == com.crdy.powergyan.domain.model.BatteryStatus.CHARGING &&
            previousLevel in 0 until policy.safeThreshold && snapshot.percentage >= policy.safeThreshold &&
            !prefs.getBoolean("target_latched", false)
        val crossedFull = snapshot.percentage >= 100 && !prefs.getBoolean("full_latched", false)
        
        if (crossedTarget || crossedFull) {
            play(policy.customSoundUri, loop = true)
            
            // Launch the full-screen Alarm Popup via callback to bypass Android 10+ background blocks!
            onTriggerAlarm?.invoke()
            
            if (crossedTarget) prefs.edit().putBoolean("target_latched", true).apply()
            if (crossedFull) prefs.edit().putBoolean("full_latched", true).apply()
        }
        
        if (snapshot.percentage < policy.safeThreshold) prefs.edit().putBoolean("target_latched", false).apply()
        if (snapshot.percentage < 100) prefs.edit().putBoolean("full_latched", false).apply()
        prefs.edit().putInt("battery_level", snapshot.percentage).apply()
    }

    private fun play(customUri: String?, loop: Boolean, isAlarm: Boolean = true) {
        stopAlarms()
        try {
            if (customUri != null) {
                val uri = android.net.Uri.parse(customUri)
                activeMediaPlayer = android.media.MediaPlayer().apply {
                    setDataSource(context, uri)
                    setAudioAttributes(android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                    isLooping = loop
                    prepare()
                }
                activeMediaPlayer?.start()
                if (!loop) activeMediaPlayer?.setOnCompletionListener { 
                    it.release()
                    activeMediaPlayer = null
                }
            } else {
                val uri = if (isAlarm) {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
                activeRingtone = RingtoneManager.getRingtone(context, uri)
                // Note: RingtoneManager doesn't natively support easy looping without API 28+ loop features.
                // We'll just let it play once or loop if device supports it automatically for TYPE_ALARM.
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && loop) {
                    activeRingtone?.isLooping = true
                }
                activeRingtone?.play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            activeRingtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            activeRingtone?.play()
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
