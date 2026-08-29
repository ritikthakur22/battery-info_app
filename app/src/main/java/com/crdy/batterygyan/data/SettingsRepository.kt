package com.crdy.batterygyan.data

import com.crdy.batterygyan.domain.model.DisplaySettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val displaySettings: Flow<DisplaySettings>

    suspend fun updateTextScale(scale: Float)
    suspend fun updateThemeMode(mode: com.crdy.batterygyan.domain.model.ThemeMode)
    suspend fun toggleSecondaryInfo(enabled: Boolean)
    // Additional update methods can be added as needed
}
