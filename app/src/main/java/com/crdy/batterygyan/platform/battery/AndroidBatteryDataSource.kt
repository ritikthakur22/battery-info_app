package com.crdy.batterygyan.platform.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.crdy.batterygyan.data.BatteryRepository
import com.crdy.batterygyan.domain.model.BatteryHealth
import com.crdy.batterygyan.domain.model.BatterySnapshot
import com.crdy.batterygyan.domain.model.BatteryStatus
import com.crdy.batterygyan.domain.model.PluggedState
import com.crdy.batterygyan.platform.alerts.BatteryAlertController
import com.crdy.batterygyan.domain.model.AlertPolicy
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

class AndroidBatteryDataSource(
    private val context: Context,
    private val alertController: BatteryAlertController? = null,
    private val alertPolicy: () -> AlertPolicy = { AlertPolicy() }
) : BatteryRepository {

    override suspend fun getBatterySnapshot(): BatterySnapshot {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter)
        return intent?.let { parseBatteryIntent(it) } ?: createUnknownSnapshot()
    }

    override fun observeBatteryState(): Flow<BatterySnapshot> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    parseBatteryIntent(intent).also { snapshot ->
                        alertController?.onSnapshot(snapshot, alertPolicy())
                        trySend(snapshot)
                    }
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }.onStart {
        // Emit immediately upon subscription
        emit(getBatterySnapshot())
    }

    private fun parseBatteryIntent(intent: Intent): BatterySnapshot {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percentage = if (level != -1 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            0
        }

        val statusInt = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val status = when (statusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryStatus.DISCHARGING
            BatteryManager.BATTERY_STATUS_FULL -> BatteryStatus.FULL
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryStatus.NOT_CHARGING
            else -> BatteryStatus.UNKNOWN
        }

        val pluggedInt = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val plugged = when (pluggedInt) {
            BatteryManager.BATTERY_PLUGGED_AC -> PluggedState.AC
            BatteryManager.BATTERY_PLUGGED_USB -> PluggedState.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> PluggedState.WIRELESS
            0 -> PluggedState.NONE
            else -> PluggedState.UNKNOWN
        }

        val healthInt = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        val health = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.UNKNOWN_FAILURE
            else -> BatteryHealth.UNKNOWN
        }

        // Temperature is in tenths of a degree Centigrade
        val tempInt = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val temperatureC = if (tempInt > 0) tempInt / 10f else null

        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1).takeIf { it > 0 }

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val currentUa = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            .takeIf { it != Int.MIN_VALUE && it != 0 }
        val chargeCounterUah = batteryManager
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            .takeIf { it > 0 }
        val energyNwh = batteryManager
            .getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            .takeIf { it > 0 }

        return BatterySnapshot(
            percentage = percentage,
            status = status,
            plugged = plugged,
            temperatureC = temperatureC,
            voltageMv = voltageMv,
            currentUa = currentUa,
            chargeCounterUah = chargeCounterUah,
            energyNwh = energyNwh,
            technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY),
            health = health,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun createUnknownSnapshot(): BatterySnapshot {
        return BatterySnapshot(
            percentage = 0,
            status = BatteryStatus.UNKNOWN,
            plugged = PluggedState.UNKNOWN,
            temperatureC = null,
            voltageMv = null,
            currentUa = null,
            chargeCounterUah = null,
            energyNwh = null,
            technology = null,
            health = BatteryHealth.UNKNOWN,
            timestamp = System.currentTimeMillis()
        )
    }
}
