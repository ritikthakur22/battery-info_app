package com.crdy.powergyan.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.crdy.powergyan.data.BatteryRepository
import com.crdy.powergyan.domain.model.BatterySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val batteryRepository: BatteryRepository
) : ViewModel() {

    private val _manualSnapshot = MutableStateFlow<BatterySnapshot?>(null)

    val batteryState: StateFlow<BatterySnapshot?> = merge(
        batteryRepository.observeBatteryState(),
        _manualSnapshot.filterNotNull()
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun refresh() {
        viewModelScope.launch {
            _manualSnapshot.value = batteryRepository.getBatterySnapshot()
        }
    }

    class Factory(private val batteryRepository: BatteryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(batteryRepository) as T
        }
    }
}
