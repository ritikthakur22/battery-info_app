package com.crdy.powergyan.domain.model

enum class BatteryStatus {
    CHARGING, DISCHARGING, FULL, NOT_CHARGING, UNKNOWN
}

enum class PluggedState {
    NONE, AC, USB, WIRELESS, UNKNOWN
}

enum class BatteryHealth {
    GOOD, COLD, DEAD, OVERHEAT, OVER_VOLTAGE, UNKNOWN_FAILURE, UNKNOWN
}

data class BatterySnapshot(
    val percentage: Int,
    val status: BatteryStatus,
    val plugged: PluggedState,
    val temperatureC: Float?,
    val voltageMv: Int?,
    val currentUa: Int?,
    val chargeCounterUah: Int?,
    val energyNwh: Long?,
    val technology: String?,
    val health: BatteryHealth?,
    val timestamp: Long,
    val cycleCount: Int? = null,
    val designCapacityMah: Int? = null
) {
    /** Full capacity estimated from the charge counter; noisy below ~50%, so only trusted above it. */
    val estimatedCapacityMah: Int?
        get() = chargeCounterUah?.takeIf { it > 0 && percentage >= 50 }?.let { (it / 10L / percentage).toInt() }
}

/** Instantaneous power in watts, or null when the phone doesn't report current/voltage. */
fun BatterySnapshot.watts(): Float? {
    val ua = currentUa?.takeIf { it != 0 } ?: return null
    val mv = voltageMv ?: return null
    return Math.abs(ua) / 1_000_000f * mv / 1000f
}

/** Estimated minutes to reach [target]% while charging, or null when it can't be estimated sensibly. */
fun BatterySnapshot.minutesTo(target: Int): Int? {
    if (status != BatteryStatus.CHARGING || percentage !in 1 until target) return null
    val currentMa = Math.abs(currentUa ?: return null) / 1000f
    val chargeMah = (chargeCounterUah ?: return null) / 1000f
    if (currentMa <= 0f || chargeMah <= 0f) return null
    val missingMah = chargeMah / percentage * (target - percentage)
    return (missingMah / currentMa * 60).toInt().takeIf { it in 1..600 }
}
