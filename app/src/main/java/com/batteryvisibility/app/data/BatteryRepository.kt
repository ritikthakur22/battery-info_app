package com.batteryvisibility.app.data

import com.batteryvisibility.app.domain.model.BatterySnapshot
import kotlinx.coroutines.flow.Flow

interface BatteryRepository {
    /**
     * Gets a one-shot current reading of the battery.
     */
    suspend fun getBatterySnapshot(): BatterySnapshot

    /**
     * Observes battery changes over time while there is an active subscriber.
     */
    fun observeBatteryState(): Flow<BatterySnapshot>
}
