package com.crdy.powergyan.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.crdy.powergyan.data.SettingsRepository
import com.crdy.powergyan.domain.model.DisplaySettings
import com.crdy.powergyan.domain.model.ThemeMode
import com.crdy.powergyan.domain.model.AccentColor
import com.crdy.powergyan.domain.model.AccessCapabilities
import com.crdy.powergyan.domain.model.ChargeLimitResult
import com.crdy.powergyan.domain.model.ChargeLimitValidator
import com.crdy.powergyan.domain.model.AlertPolicy
import com.crdy.powergyan.platform.access.CapabilityDetector
import com.crdy.powergyan.platform.access.ChargeControlProvider
import kotlinx.coroutines.flow.MutableStateFlow
import rikka.shizuku.Shizuku
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val capabilityDetector: CapabilityDetector,
    private val chargeControlProvider: ChargeControlProvider
) : ViewModel() {

    val capabilities = MutableStateFlow(AccessCapabilities())
    val chargeLimitResult = MutableStateFlow<ChargeLimitResult?>(null)

    init { refreshCapabilities() }

    val displaySettings: StateFlow<DisplaySettings> = settingsRepository.displaySettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DisplaySettings()
        )

    fun setTextScale(scale: Float) {
        viewModelScope.launch { settingsRepository.updateTextScale(scale) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.updateThemeMode(mode) }
    }

    fun setIconScale(scale: Float) {
        viewModelScope.launch { settingsRepository.updateIconScale(scale) }
    }

    fun setAccentColor(color: AccentColor) {
        viewModelScope.launch { settingsRepository.updateAccentColor(color) }
    }

    fun updateSmartChargeConfig(config: com.crdy.powergyan.domain.model.SmartChargeConfig) {
        viewModelScope.launch { settingsRepository.updateSmartChargeConfig(config) }
    }

    fun setAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAlertPolicy(displaySettings.value.alertPolicy.copy(enabled = enabled)) }
    }

    fun setAlertThreshold(value: Float) {
        viewModelScope.launch { settingsRepository.updateAlertPolicy(displaySettings.value.alertPolicy.copy(threshold = value.toInt())) }
    }

    fun setCustomSoundUri(uri: String?) {
        viewModelScope.launch { settingsRepository.updateAlertPolicy(displaySettings.value.alertPolicy.copy(customSoundUri = uri)) }
    }

    fun setPlugSoundUri(uri: String?) {
        viewModelScope.launch { settingsRepository.updateAlertPolicy(displaySettings.value.alertPolicy.copy(plugSoundUri = uri)) }
    }

    fun setUnplugSoundUri(uri: String?) {
        viewModelScope.launch { settingsRepository.updateAlertPolicy(displaySettings.value.alertPolicy.copy(unplugSoundUri = uri)) }
    }

    fun refreshCapabilities() {
        viewModelScope.launch { capabilities.value = capabilityDetector.detect() }
    }

    fun requestShizukuPermission() {
        if (capabilities.value.shizukuAvailable && !capabilities.value.shizukuPermissionGranted) {
            runCatching { Shizuku.requestPermission(1001) }
        }
    }

    fun setChargeLimit(stopAt: Int, resumeAt: Int) {
        val error = ChargeLimitValidator.validate(stopAt, resumeAt)
        if (error != null) {
            chargeLimitResult.value = ChargeLimitResult(false, error)
            return
        }
        viewModelScope.launch { chargeLimitResult.value = chargeControlProvider.writeStopLimit(stopAt) }
    }

    fun resetChargeLimit() {
        viewModelScope.launch { chargeLimitResult.value = chargeControlProvider.reset() }
    }

    fun toggleSecondaryInfo(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.toggleSecondaryInfo(enabled) }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
            chargeLimitResult.value = chargeControlProvider.reset()
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val capabilityDetector: CapabilityDetector,
        private val chargeControlProvider: ChargeControlProvider
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository, capabilityDetector, chargeControlProvider) as T
        }
    }
}
