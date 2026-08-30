package com.crdy.powergyan.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.crdy.powergyan.data.SettingsRepository
import com.crdy.powergyan.domain.model.DisplaySettings
import com.crdy.powergyan.domain.model.AccentColor
import com.crdy.powergyan.domain.model.AlertPolicy
import com.crdy.powergyan.domain.model.SmartChargeConfig
import com.crdy.powergyan.domain.model.ThemeMode
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
        val CUSTOM_SOUND_URI = stringPreferencesKey("custom_sound_uri")
        val SC_ENABLED = booleanPreferencesKey("sc_enabled")
        val SC_STOP = androidx.datastore.preferences.core.intPreferencesKey("sc_stop")
        val SC_RESUME = androidx.datastore.preferences.core.intPreferencesKey("sc_resume")
        val SC_PATH = stringPreferencesKey("sc_path")
        val SC_ENABLE_VAL = stringPreferencesKey("sc_enable_val")
        val SC_DISABLE_VAL = stringPreferencesKey("sc_disable_val")
        val SC_TEMP_ENABLED = booleanPreferencesKey("sc_temp_enabled")
        val SC_TEMP_MAX = androidx.datastore.preferences.core.intPreferencesKey("sc_temp_max")
        val PLUG_SOUND_URI = stringPreferencesKey("plug_sound_uri")
        val UNPLUG_SOUND_URI = stringPreferencesKey("unplug_sound_uri")
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
            } ?: AccentColor.BLUE
            val alertPolicy = AlertPolicy(
                enabled = preferences[PreferencesKeys.ALERT_ENABLED] ?: false,
                threshold = (preferences[PreferencesKeys.ALERT_THRESHOLD] ?: 20f).toInt(),
                customSoundUri = preferences[PreferencesKeys.CUSTOM_SOUND_URI],
                plugSoundUri = preferences[PreferencesKeys.PLUG_SOUND_URI],
                unplugSoundUri = preferences[PreferencesKeys.UNPLUG_SOUND_URI]
            )
            val configuredPath = preferences[PreferencesKeys.SC_PATH] ?: "/sys/class/power_supply/battery/input_suspend"
            val configuredEnable = preferences[PreferencesKeys.SC_ENABLE_VAL] ?: "0"
            val configuredDisable = preferences[PreferencesKeys.SC_DISABLE_VAL] ?: "1"
            val inputSuspendDefaultsWereInherited = configuredPath.endsWith("/input_suspend") &&
                configuredEnable == "1" && configuredDisable == "0"
            val smartChargeConfig = SmartChargeConfig(
                enabled = preferences[PreferencesKeys.SC_ENABLED] ?: false,
                stopLimit = preferences[PreferencesKeys.SC_STOP] ?: 80,
                resumeLimit = preferences[PreferencesKeys.SC_RESUME] ?: 75,
                ctrlPath = configuredPath,
                ctrlEnable = if (inputSuspendDefaultsWereInherited) "0" else configuredEnable,
                ctrlDisable = if (inputSuspendDefaultsWereInherited) "1" else configuredDisable,
                tempControlEnabled = preferences[PreferencesKeys.SC_TEMP_ENABLED] ?: false,
                maxTempC = preferences[PreferencesKeys.SC_TEMP_MAX] ?: 40
            )

            DisplaySettings(
                textScale = textScale,
                iconScale = iconScale,
                alignment = com.crdy.powergyan.domain.model.AlignmentOption.CENTER,
                themeMode = themeMode,
                textColorMode = com.crdy.powergyan.domain.model.ColorMode.THEME,
                iconColorMode = com.crdy.powergyan.domain.model.ColorMode.THEME,
                backgroundStyle = com.crdy.powergyan.domain.model.BackgroundStyle.SURFACE,
                accentColor = accentColor,
                alertPolicy = alertPolicy,
                smartChargeConfig = smartChargeConfig,
                secondaryInfoEnabled = secondaryInfo
            )
        }

    override suspend fun updateTextScale(scale: Float) {
        context.dataStore.edit { preferences ->
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
            if (policy.customSoundUri != null) preferences[PreferencesKeys.CUSTOM_SOUND_URI] = policy.customSoundUri else preferences.remove(PreferencesKeys.CUSTOM_SOUND_URI)
            if (policy.plugSoundUri != null) preferences[PreferencesKeys.PLUG_SOUND_URI] = policy.plugSoundUri else preferences.remove(PreferencesKeys.PLUG_SOUND_URI)
            if (policy.unplugSoundUri != null) preferences[PreferencesKeys.UNPLUG_SOUND_URI] = policy.unplugSoundUri else preferences.remove(PreferencesKeys.UNPLUG_SOUND_URI)
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

    override suspend fun updateSmartChargeConfig(config: SmartChargeConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SC_ENABLED] = config.enabled
            preferences[PreferencesKeys.SC_STOP] = config.stopLimit
            preferences[PreferencesKeys.SC_RESUME] = config.resumeLimit
            preferences[PreferencesKeys.SC_PATH] = config.ctrlPath
            preferences[PreferencesKeys.SC_ENABLE_VAL] = config.ctrlEnable
            preferences[PreferencesKeys.SC_DISABLE_VAL] = config.ctrlDisable
            preferences[PreferencesKeys.SC_TEMP_ENABLED] = config.tempControlEnabled
            preferences[PreferencesKeys.SC_TEMP_MAX] = config.maxTempC
        }
    }

    override suspend fun resetToDefaults() {
        context.dataStore.edit { it.clear() }
    }
}
