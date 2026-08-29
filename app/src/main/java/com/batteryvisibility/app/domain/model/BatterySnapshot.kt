package com.batteryvisibility.app.domain.model

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
    val health: BatteryHealth?,
    val timestamp: Long
)
