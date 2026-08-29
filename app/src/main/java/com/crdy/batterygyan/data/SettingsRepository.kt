package com.crdy.batterygyan.data

import com.crdy.batterygyan.domain.model.DisplaySettings
import com.crdy.batterygyan.domain.model.AccentColor
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val displaySettings: Flow<DisplaySettings>

    suspend fun updateTextScale(scale: Float)
    suspend fun updateIconScale(scale: Float)
    suspend fun updateAccentColor(color: AccentColor)
    suspend fun updateAlertPolicy(policy: com.crdy.batterygyan.domain.model.AlertPolicy)
    suspend fun updateThemeMode(mode: com.crdy.batterygyan.domain.model.ThemeMode)
    suspend fun toggleSecondaryInfo(enabled: Boolean)
    // Additional update methods can be added as needed
}
