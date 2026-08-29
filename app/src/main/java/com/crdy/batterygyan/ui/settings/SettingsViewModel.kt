package com.crdy.batterygyan.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.crdy.batterygyan.data.SettingsRepository
import com.crdy.batterygyan.domain.model.DisplaySettings
import com.crdy.batterygyan.domain.model.ThemeMode
import com.crdy.batterygyan.domain.model.AccentColor
import com.crdy.batterygyan.domain.model.AccessCapabilities
import com.crdy.batterygyan.domain.model.ChargeLimitResult
import com.crdy.batterygyan.domain.model.ChargeLimitValidator
import com.crdy.batterygyan.domain.model.AlertPolicy
import com.crdy.batterygyan.platform.access.CapabilityDetector
import com.crdy.batterygyan.platform.access.ChargeControlProvider
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
        viewModelScope.launch {
            settingsRepository.updateTextScale(scale)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(mode)
        }
    }

    fun setIconScale(scale: Float) {
        viewModelScope.launch { settingsRepository.updateIconScale(scale) }
    }

    fun setAccentColor(color: AccentColor) {
        viewModelScope.launch { settingsRepository.updateAccentColor(color) }
    }

    fun setAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAlertPolicy(displaySettings.value.alertPolicy.copy(enabled = enabled)) }
    }

    fun setAlertThreshold(value: Float) {
        viewModelScope.launch { settingsRepository.updateAlertPolicy(displaySettings.value.alertPolicy.copy(threshold = value.toInt())) }
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
        viewModelScope.launch {
            settingsRepository.toggleSecondaryInfo(enabled)
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
