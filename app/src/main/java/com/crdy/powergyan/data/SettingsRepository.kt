package com.crdy.powergyan.data

import com.crdy.powergyan.domain.model.DisplaySettings
import com.crdy.powergyan.domain.model.AccentColor
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val displaySettings: Flow<DisplaySettings>

    suspend fun updateTextScale(scale: Float)
    suspend fun updateIconScale(scale: Float)
    suspend fun updateAccentColor(color: AccentColor)
    suspend fun updateAlertPolicy(policy: com.crdy.powergyan.domain.model.AlertPolicy)
    suspend fun updateSmartChargeConfig(config: com.crdy.powergyan.domain.model.SmartChargeConfig)
    suspend fun resetToDefaults()
    suspend fun updateThemeMode(mode: com.crdy.powergyan.domain.model.ThemeMode)
    suspend fun toggleSecondaryInfo(enabled: Boolean)
    // Additional update methods can be added as needed
}
