package com.crdy.batterygyan.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.crdy.batterygyan.data.SettingsRepository
import com.crdy.batterygyan.domain.model.DisplaySettings
import com.crdy.batterygyan.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "display_settings")

class SettingsDataStore(private val context: Context) : SettingsRepository {

    private object PreferencesKeys {
        val TEXT_SCALE = floatPreferencesKey("text_scale")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SECONDARY_INFO = booleanPreferencesKey("secondary_info")
    }

    override val displaySettings: Flow<DisplaySettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val textScale = preferences[PreferencesKeys.TEXT_SCALE] ?: 1.0f
            val themeModeName = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            val themeMode = try {
                ThemeMode.valueOf(themeModeName)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
            val secondaryInfo = preferences[PreferencesKeys.SECONDARY_INFO] ?: true

            DisplaySettings(
                textScale = textScale,
                themeMode = themeMode,
                secondaryInfoEnabled = secondaryInfo
            )
        }

    override suspend fun updateTextScale(scale: Float) {
        context.dataStore.edit { preferences ->
            // Validate input: safe limits for accessibility
            preferences[PreferencesKeys.TEXT_SCALE] = scale.coerceIn(0.8f, 2.0f)
        }
    }

    override suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    override suspend fun toggleSecondaryInfo(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_INFO] = enabled
        }
    }
}
