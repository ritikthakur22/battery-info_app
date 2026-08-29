package com.crdy.batterygyan.domain.model

data class AlertPolicy(
    val enabled: Boolean = false,
    val threshold: Int = 20,
    val cableAlerts: Boolean = false
) {
    val safeThreshold: Int get() = threshold.coerceIn(1, 99)
}
