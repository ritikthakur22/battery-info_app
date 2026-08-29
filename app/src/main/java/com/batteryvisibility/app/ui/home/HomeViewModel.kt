package com.batteryvisibility.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.batteryvisibility.app.data.BatteryRepository
import com.batteryvisibility.app.domain.model.BatterySnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    batteryRepository: BatteryRepository
) : ViewModel() {

    val batteryState: StateFlow<BatterySnapshot?> = batteryRepository.observeBatteryState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    class Factory(private val batteryRepository: BatteryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(batteryRepository) as T
        }
    }
}
