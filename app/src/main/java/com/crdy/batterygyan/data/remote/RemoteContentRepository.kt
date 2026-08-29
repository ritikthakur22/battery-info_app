package com.crdy.batterygyan.data.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Announcement(val id: String, val title: String, val message: String, val publishedAt: String?)
data class CompatibilityInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String?,
    val rom: String?,
    val chargeLimitSupport: Boolean,
    val knownLimitations: List<String>,
    val recommendedAccessMethod: String?,
    val notes: String?
)

/** Informational-only HTTPS JSON. It never supplies executable code or shell commands. */
class RemoteContentRepository(private val context: Context) {
    companion object {
        private const val MAX_BYTES = 512 * 1024
        private const val ANNOUNCEMENTS = "https://raw.githubusercontent.com/ritikthakur22/battery-info_app/main/remote/announcements.json"
        private const val COMPATIBILITY = "https://raw.githubusercontent.com/ritikthakur22/battery-info_app/main/remote/compatibility.json"
    }

    suspend fun fetchAnnouncements(): List<Announcement> = withContext(Dispatchers.IO) {
        parseAnnouncements(fetch(ANNOUNCEMENTS, "announcements"))
    }

    suspend fun fetchCompatibility(): List<CompatibilityInfo> = withContext(Dispatchers.IO) {
        parseCompatibility(fetch(COMPATIBILITY, "compatibility"))
    }

    private fun fetch(endpoint: String, cacheKey: String): String? = runCatching {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4000
            readTimeout = 4000
            setRequestProperty("Accept", "application/json")
        }
        if (connection.responseCode !in 200..299) return@runCatching null
        connection.inputStream.use { stream ->
            val bytes = stream.readBytes()
            if (bytes.size > MAX_BYTES) null else bytes.toString(Charsets.UTF_8).also {
                context.getSharedPreferences("remote_cache", Context.MODE_PRIVATE).edit().putString(cacheKey, it).apply()
            }
        }
    }.getOrNull() ?: context.getSharedPreferences("remote_cache", Context.MODE_PRIVATE).getString(cacheKey, null)

    private fun parseAnnouncements(json: String?): List<Announcement> = runCatching {
        val root = JSONObject(json ?: return@runCatching emptyList())
        if (root.optInt("schemaVersion", 0) != 1) return@runCatching emptyList()
        val array = root.optJSONArray("announcements") ?: JSONArray()
        (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { item ->
                val id = item.optString("id")
                val title = item.optString("title")
                val message = item.optString("message")
                if (id.isBlank() || title.isBlank() || message.isBlank()) null
                else Announcement(id, title, message, item.optString("publishedAt").ifBlank { null })
            }
        }
    }.getOrDefault(emptyList())

    private fun parseCompatibility(json: String?): List<CompatibilityInfo> = runCatching {
        val root = JSONObject(json ?: return@runCatching emptyList())
        if (root.optInt("schemaVersion", 0) != 1) return@runCatching emptyList()
        val array = root.optJSONArray("devices") ?: JSONArray()
        (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let { item ->
            val manufacturer = item.optString("manufacturer")
            val model = item.optString("model")
            if (manufacturer.isBlank() || model.isBlank()) null else CompatibilityInfo(
                manufacturer, model, item.optString("androidVersion").ifBlank { null },
                item.optString("rom").ifBlank { null }, item.optBoolean("chargeLimitSupport"),
                item.optJSONArray("knownLimitations")?.let { values -> (0 until values.length()).map { values.optString(it) } } ?: emptyList(),
                item.optString("recommendedAccessMethod").ifBlank { null }, item.optString("notes").ifBlank { null }
            )
        } }
    }.getOrDefault(emptyList())
}
