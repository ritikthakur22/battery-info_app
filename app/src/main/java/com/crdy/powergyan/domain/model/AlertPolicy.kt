package com.crdy.powergyan.domain.model

data class AlertPolicy(
    val enabled: Boolean = false,
    val threshold: Int = 20,
    val cableAlerts: Boolean = false,
    val customSoundUri: String? = null,
    val plugSoundUri: String? = null,
    val unplugSoundUri: String? = null,
    /** Warn once when the battery drops to this level while on battery; 0 means off. */
    val lowThreshold: Int = 0
) {
    val safeThreshold: Int get() = threshold.coerceIn(1, 99)
}
