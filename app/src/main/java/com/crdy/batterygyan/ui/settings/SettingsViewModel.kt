package com.crdy.batterygyan.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.crdy.batterygyan.data.SettingsRepository
import com.crdy.batterygyan.domain.model.DisplaySettings
import com.crdy.batterygyan.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

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

    fun toggleSecondaryInfo(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleSecondaryInfo(enabled)
        }
    }

    class Factory(private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository) as T
        }
    }
}
