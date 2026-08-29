package com.crdy.batterygyan.platform.access

import android.content.Context
import android.content.pm.PackageManager
import com.crdy.batterygyan.domain.model.AccessCapabilities
import rikka.shizuku.Shizuku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class CapabilityDetector(private val context: Context) {
    suspend fun detect(): AccessCapabilities = withContext(Dispatchers.IO) {
        val root = rootProbe()
        val shizuku = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val shizukuPermission = if (shizuku) runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false) else false
        AccessCapabilities(
            rootAvailable = root,
            shizukuAvailable = shizuku,
            shizukuPermissionGranted = shizukuPermission,
            diagnostics = root || shizukuPermission,
            chargeLimit = root
        )
    }

    private fun rootProbe(): Boolean = runCatching {
        val process = ProcessBuilder("su", "-c", "id")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0 &&
            Regex("(^|\\s)uid=0(\\(|\\s)").containsMatchIn(output)
    }.getOrDefault(false)
}
