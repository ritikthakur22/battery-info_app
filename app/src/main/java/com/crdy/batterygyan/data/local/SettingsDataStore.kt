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
import com.crdy.batterygyan.domain.model.AccentColor
import com.crdy.batterygyan.domain.model.AlertPolicy
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
        val ICON_SCALE = floatPreferencesKey("icon_scale")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val ALERT_ENABLED = booleanPreferencesKey("alert_enabled")
        val ALERT_THRESHOLD = floatPreferencesKey("alert_threshold")
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
            val iconScale = preferences[PreferencesKeys.ICON_SCALE] ?: 1.0f
            val accentColor = preferences[PreferencesKeys.ACCENT_COLOR]?.let {
                runCatching { AccentColor.valueOf(it) }.getOrNull()
            } ?: AccentColor.MINT
            val alertPolicy = AlertPolicy(
                enabled = preferences[PreferencesKeys.ALERT_ENABLED] ?: false,
                threshold = (preferences[PreferencesKeys.ALERT_THRESHOLD] ?: 20f).toInt()
            )

            DisplaySettings(
                textScale = textScale,
                iconScale = iconScale,
                accentColor = accentColor,
                alertPolicy = alertPolicy,
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

    override suspend fun updateIconScale(scale: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ICON_SCALE] = scale.coerceIn(0.8f, 1.6f)
        }
    }

    override suspend fun updateAccentColor(color: AccentColor) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCENT_COLOR] = color.name
        }
    }

    override suspend fun updateAlertPolicy(policy: AlertPolicy) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALERT_ENABLED] = policy.enabled
            preferences[PreferencesKeys.ALERT_THRESHOLD] = policy.safeThreshold.toFloat()
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
