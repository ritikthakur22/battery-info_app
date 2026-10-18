package com.crdy.powergyan.platform.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.crdy.powergyan.data.BatteryRepository
import com.crdy.powergyan.domain.model.BatteryHealth
import com.crdy.powergyan.domain.model.BatterySnapshot
import com.crdy.powergyan.domain.model.BatteryStatus
import com.crdy.powergyan.domain.model.PluggedState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart


    private fun getCycleCount(context: Context): Int? {
        
        // Try direct sysfs read (works on some OEMs without root)
        runCatching {
            val file = java.io.File("/sys/class/power_supply/battery/cycle_count")
            if (file.exists() && file.canRead()) {
                val text = file.readText().trim()
                val cycles = text.toInt()
                if (cycles > 0) return cycles
            }
        }
        
        // Try root shell if direct read fails
        runCatching {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat /sys/class/power_supply/battery/cycle_count"))
            val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
            val text = reader.readLine()?.trim()
            process.waitFor()
            if (!text.isNullOrEmpty()) {
                val cycles = text.toInt()
                if (cycles > 0) return cycles
            }
        }
        
        return null
    }

class AndroidBatteryDataSource(private val context: Context) : BatteryRepository {
    // shortcut: sysfs/su fallback read once per instance; stale until app restart, fine since cycles change slowly.
    private val fallbackCycleCount: Int? by lazy { getCycleCount(context) }
    private val designCapacityMah: Int? by lazy { readDesignCapacity() }

    private fun readDesignCapacity(): Int? {
        runCatching {
            val uah = java.io.File("/sys/class/power_supply/battery/charge_full_design").readText().trim().toLong()
            if (uah > 0) return (if (uah > 100_000) uah / 1000 else uah).toInt()
        }
        // Hidden but long-stable API that most battery apps rely on; fails quietly if blocked.
        return runCatching {
            val profile = Class.forName("com.android.internal.os.PowerProfile").getConstructor(Context::class.java).newInstance(context)
            (profile.javaClass.getMethod("getBatteryCapacity").invoke(profile) as Double).toInt()
        }.getOrNull()?.takeIf { it > 500 }
    }

    override suspend fun getBatterySnapshot(): BatterySnapshot {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter)
        return intent?.let { parseBatteryIntent(it) } ?: createUnknownSnapshot()
    }

    override fun observeBatteryState(): Flow<BatterySnapshot> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    trySend(parseBatteryIntent(intent))
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
        // shortcut: some kernels report mA instead of µA; anything under 10 mA-as-µA is treated as mA.
        val currentUa = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            .takeIf { it != Int.MIN_VALUE && it != 0 }
            ?.let { if (Math.abs(it) < 10_000) it * 1000 else it }
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
            timestamp = System.currentTimeMillis(),
            cycleCount = intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1).takeIf { it > 0 } ?: fallbackCycleCount,
            designCapacityMah = designCapacityMah
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
            timestamp = System.currentTimeMillis(),
            cycleCount = fallbackCycleCount
        )
    }
}
