package com.crdy.batterygyan.platform.access

import com.crdy.batterygyan.domain.model.ChargeLimitResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

interface ChargeControlProvider {
    suspend fun isSupported(): Boolean
    suspend fun readStopLimit(): Int?
    suspend fun writeStopLimit(value: Int): ChargeLimitResult
    suspend fun reset(): ChargeLimitResult
}

/**
 * Conservative generic provider. It only considers known, fixed sysfs paths
 * and verifies every write by reading the value back. No shell input is user-built.
 */
class GenericSysfsChargeControlProvider : ChargeControlProvider {
    private val candidates = listOf(
        "/sys/class/power_supply/battery/charge_control_end_threshold",
        "/sys/devices/platform/soc/800f000.qcom,spmi/spmi-0/spmi0-00/c440000.qcom,qpnp-smb5/power_supply/battery/charge_control_end_threshold"
    )

    override suspend fun isSupported(): Boolean = withContext(Dispatchers.IO) {
        candidates.any { path -> File(path).exists() && rootRead(path) != null }
    }

    override suspend fun readStopLimit(): Int? = withContext(Dispatchers.IO) {
        candidates.firstNotNullOfOrNull { rootRead(it)?.trim()?.toIntOrNull() }
    }

    override suspend fun writeStopLimit(value: Int): ChargeLimitResult = withContext(Dispatchers.IO) {
        val path = candidates.firstOrNull { File(it).exists() }
            ?: return@withContext ChargeLimitResult(false, "No supported charge-limit interface was found.")
        if (value !in 2..99) return@withContext ChargeLimitResult(false, "Stop limit must be between 2% and 99%.")
        val result = rootCommand("printf '%d' $value > '$path'")
        val actual = rootRead(path)?.trim()?.toIntOrNull()
        if (result && actual == value) ChargeLimitResult(true, "Charge limit verified at $value%.")
        else ChargeLimitResult(false, "Battery Gyan could not verify the requested charging limit.")
    }

    override suspend fun reset(): ChargeLimitResult = withContext(Dispatchers.IO) {
        val path = candidates.firstOrNull { File(it).exists() }
            ?: return@withContext ChargeLimitResult(false, "No supported charge-limit interface was found.")
        if (rootCommand("printf '%d' 100 > '$path'")) {
            val actual = rootRead(path)?.trim()?.toIntOrNull()
            if (actual == 100) ChargeLimitResult(true, "Charging control reset to normal.")
            else ChargeLimitResult(false, "Reset could not be verified.")
        } else ChargeLimitResult(false, "Reset command was not accepted.")
    }

    private fun rootRead(path: String): String? = if (File(path).exists()) {
        runCommand("su", "-c", "cat '$path'")
    } else null

    private fun rootCommand(command: String): Boolean = runCommand("su", "-c", command) != null

    private fun runCommand(vararg command: String): String? = runCatching {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0) output else null
    }.getOrNull()
}
