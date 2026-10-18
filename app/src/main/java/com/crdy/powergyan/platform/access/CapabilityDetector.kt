package com.crdy.powergyan.platform.access

import com.crdy.powergyan.domain.model.AccessCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class CapabilityDetector {
    suspend fun detect(): AccessCapabilities = withContext(Dispatchers.IO) {
        AccessCapabilities(rootAvailable = rootProbe())
    }

    private fun rootProbe(): Boolean = runCatching {
        val process = ProcessBuilder("su", "-c", "id")
            .redirectErrorStream(true)
            .start()
        if (!process.waitFor(2, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return@runCatching false
        }
        val output = process.inputStream.bufferedReader().use { it.readText() }
        process.exitValue() == 0 &&
            Regex("(^|\\s)uid=0(\\(|\\s)").containsMatchIn(output)
    }.getOrDefault(false)
}
