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
    val maxTempC: Int = 40
)
