package com.crdy.powergyan.domain.model

enum class AccessMethod { AUTOMATIC, ROOT, SHIZUKU }

data class AccessCapabilities(
    val rootAvailable: Boolean = false,
    val shizukuAvailable: Boolean = false,
    val shizukuPermissionGranted: Boolean = false,
    val diagnostics: Boolean = false,
    val chargeLimit: Boolean = false
)

data class ChargeLimitConfig(
    val stopAt: Int = 80,
    val resumeAt: Int = 75,
    val enabled: Boolean = false
)

data class ChargeLimitResult(val success: Boolean, val message: String)

object ChargeLimitValidator {
    fun validate(stopAt: Int, resumeAt: Int): String? = when {
        resumeAt !in 1..98 -> "Resume threshold must be between 1% and 98%."
        stopAt !in 2..99 -> "Stop threshold must be between 2% and 99%."
        resumeAt >= stopAt -> "Resume threshold must be lower than stop threshold."
        else -> null
    }
}
