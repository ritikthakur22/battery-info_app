package com.crdy.powergyan.domain.model

data class AlertPolicy(
    val enabled: Boolean = false,
    val threshold: Int = 20,
    val cableAlerts: Boolean = false,
    val customSoundUri: String? = null,
    val plugSoundUri: String? = null,
    val unplugSoundUri: String? = null
) {
    val safeThreshold: Int get() = threshold.coerceIn(1, 99)
}
