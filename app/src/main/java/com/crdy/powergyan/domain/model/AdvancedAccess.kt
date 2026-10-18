package com.crdy.powergyan.domain.model

data class AccessCapabilities(val rootAvailable: Boolean = false)

data class ChargeLimitResult(val success: Boolean, val message: String)
