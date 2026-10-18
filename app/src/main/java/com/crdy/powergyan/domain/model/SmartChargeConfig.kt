package com.crdy.powergyan.domain.model

data class SmartChargeConfig(
    val enabled: Boolean = false,
    val stopLimit: Int = 80,
    val resumeLimit: Int = 75,
    val ctrlPath: String = "/sys/class/power_supply/battery/input_suspend",
    val ctrlEnable: String = "0",
    val ctrlDisable: String = "1",
    val applyOnBoot: Boolean = true,
    val tempControlEnabled: Boolean = false,
    val maxTempC: Int = 40,
    /** Separate stop level on wireless chargers, which run hotter; 0 means use [stopLimit]. */
    val wirelessStopLimit: Int = 0,
    /** Minute of the day to be at 100% by (e.g. 7:00 = 420); -1 means off. */
    val fullByMinute: Int = -1
) {
    /** The stop/resume levels that apply right now, after charger profile and the full-by time. */
    fun limitsFor(plugged: PluggedState, minuteOfDay: Int): Pair<Int, Int> {
        if (fullByMinute >= 0 && minutesUntil(fullByMinute, minuteOfDay) <= TOP_UP_WINDOW_MIN) return 100 to resumeLimit
        val stop = if (plugged == PluggedState.WIRELESS && wirelessStopLimit > 0) wirelessStopLimit else stopLimit
        return stop to minOf(resumeLimit, stop - 1)
    }

    companion object {
        // shortcut: fixed 2 h top-up before the full-by time; estimate from charge speed if users find it early/late.
        const val TOP_UP_WINDOW_MIN = 120

        fun minutesUntil(target: Int, now: Int) = (target - now + 24 * 60) % (24 * 60)
    }
}
