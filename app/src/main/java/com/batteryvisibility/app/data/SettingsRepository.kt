package com.batteryvisibility.app.data

import com.batteryvisibility.app.domain.model.DisplaySettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val displaySettings: Flow<DisplaySettings>

    suspend fun updateTextScale(scale: Float)
    suspend fun updateThemeMode(mode: com.batteryvisibility.app.domain.model.ThemeMode)
    suspend fun toggleSecondaryInfo(enabled: Boolean)
    // Additional update methods can be added as needed
}
