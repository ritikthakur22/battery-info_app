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
import com.crdy.powergyan.domain.model.AlertPolicy
import com.crdy.powergyan.platform.access.CapabilityDetector
import com.crdy.powergyan.data.BatteryRepository
import com.crdy.powergyan.domain.model.BatteryStatus
import com.crdy.powergyan.domain.model.PluggedState
import com.crdy.powergyan.platform.access.ChargeSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val capabilityDetector: CapabilityDetector,
    private val batteryRepository: BatteryRepository
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

    fun setLowThreshold(value: Int) {
        viewModelScope.launch { settingsRepository.updateAlertPolicy(displaySettings.value.alertPolicy.copy(lowThreshold = value)) }
    }

    /** Turns charging back on through the same control file the service uses. */
    fun resetChargeLimit() {
        val config = displaySettings.value.smartChargeConfig
        viewModelScope.launch { chargeLimitResult.value = restoreCharging(config) }
    }

    private suspend fun restoreCharging(config: com.crdy.powergyan.domain.model.SmartChargeConfig) = withContext(Dispatchers.IO) {
        val target = ChargeSwitch.detect(config) ?: return@withContext null
        if (ChargeSwitch.set(target, true)) ChargeLimitResult(true, "Charging is back to normal.")
        else ChargeLimitResult(false, "Couldn't turn charging back on. Restarting the phone always restores it.")
    }

    /** Pauses charging for a few seconds and checks that Android sees it stop. */
    fun testChargeControl() {
        val config = displaySettings.value.smartChargeConfig
        viewModelScope.launch {
            chargeLimitResult.value = ChargeLimitResult(true, "Testing. Keep the charger plugged in…")
            chargeLimitResult.value = withContext(Dispatchers.IO) { runChargeTest(config) }
        }
    }

    private suspend fun runChargeTest(config: com.crdy.powergyan.domain.model.SmartChargeConfig): ChargeLimitResult {
        val before = batteryRepository.getBatterySnapshot()
        if (before.plugged == PluggedState.NONE) return ChargeLimitResult(false, "Plug in your charger, then run the test.")
        if (before.percentage >= 100) return ChargeLimitResult(false, "Battery is 100% full. Disconnect or discharge below 100% to run test.")
        val target = ChargeSwitch.detect(config) ?: return ChargeLimitResult(false, "No charging control file found. Set one manually below.")
        ChargeSwitch.testInProgress = true
        try {
            if (!ChargeSwitch.set(target, false)) {
                return ChargeLimitResult(false, "Root write to ${target.path} failed. Ensure root access is granted in Magisk/KernelSU.")
            }
            delay(2500)
            val isTargetDisabled = ChargeSwitch.isCharging(target) == false
            val kernelStatus = ChargeSwitch.readKernelStatus()
            val kernelStopped = kernelStatus?.contains("Discharg", ignoreCase = true) == true ||
                    kernelStatus?.contains("Not charging", ignoreCase = true) == true
            val androidStopped = batteryRepository.getBatterySnapshot().status != BatteryStatus.CHARGING

            val stopped = isTargetDisabled || kernelStopped || androidStopped
            return if (stopped) {
                ChargeLimitResult(true, "Works! Charging paused successfully via ${target.path}.")
            } else {
                ChargeLimitResult(false, "Charging didn't stop with ${target.path}. Try another control file.")
            }
        } finally {
            ChargeSwitch.set(target, true)
            ChargeSwitch.testInProgress = false
        }
    }

    suspend fun exportSettings(): String = settingsRepository.exportBackup()

    suspend fun importSettings(json: String): Boolean = settingsRepository.importBackup(json)

    fun toggleSecondaryInfo(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.toggleSecondaryInfo(enabled) }
    }

    fun resetAllSettings() {
        val config = displaySettings.value.smartChargeConfig
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
            chargeLimitResult.value = restoreCharging(config)
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val capabilityDetector: CapabilityDetector,
        private val batteryRepository: BatteryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository, capabilityDetector, batteryRepository) as T
        }
    }
}
