package com.crdy.powergyan.data.local.history

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.crdy.powergyan.domain.model.BatterySnapshot
import com.crdy.powergyan.domain.model.watts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BatteryHistoryHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private val statePrefs = context.getSharedPreferences("battery_history_state", Context.MODE_PRIVATE)

    companion object {
        const val DATABASE_NAME = "battery_history.db"
        const val DATABASE_VERSION = 2
        const val TABLE_NAME = "history"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_LEVEL = "level"
        const val COL_IS_CHARGING = "is_charging"
        const val COL_POWER_MW = "power_mw"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_NAME (" +
                    "$COL_TIMESTAMP INTEGER PRIMARY KEY," +
                    "$COL_LEVEL INTEGER," +
                    "$COL_IS_CHARGING INTEGER," +
                    "$COL_POWER_MW INTEGER)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    /** Stores the snapshot if level or charging state changed; returns true when a row was added. */
    suspend fun insertSnapshot(snapshot: BatterySnapshot): Boolean = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val charging = snapshot.status == com.crdy.powergyan.domain.model.BatteryStatus.CHARGING
        val wasCharging = statePrefs.getBoolean("was_charging", false)
        if (charging && !wasCharging) {
            statePrefs.edit().putLong("last_charge_started", System.currentTimeMillis()).apply()
        }
        statePrefs.edit().putBoolean("was_charging", charging).apply()
        // Several screens and the service report the same broadcast; only store real changes.
        val last = statePrefs.getInt("last_row", -1)
        val row = snapshot.percentage * 2 + if (charging) 1 else 0
        if (last == row) {
            db.close()
            return@withContext false
        }
        statePrefs.edit().putInt("last_row", row).apply()
        val values = ContentValues().apply {
            put(COL_TIMESTAMP, System.currentTimeMillis())
            put(COL_LEVEL, snapshot.percentage)
            put(COL_IS_CHARGING, if (charging) 1 else 0)
            snapshot.watts()?.let { put(COL_POWER_MW, (it * 1000).toInt()) }
        }
        db.insert(TABLE_NAME, null, values)
        
        // Prune old data (keep last 24 hours)
        val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        db.delete(TABLE_NAME, "$COL_TIMESTAMP < ?", arrayOf(cutoff.toString()))
        db.close()
        true
    }

    fun getLastChargeStarted(): Long? = statePrefs.getLong("last_charge_started", 0L).takeIf { it > 0L }

    suspend fun getHistory(): List<HistoryPoint> = withContext(Dispatchers.IO) { getHistoryBlocking() }

    /** For callers already off the main thread, like the widget's composition. */
    fun getHistoryBlocking(): List<HistoryPoint> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME, 
            arrayOf(COL_TIMESTAMP, COL_LEVEL, COL_IS_CHARGING, COL_POWER_MW),
            null, null, null, null, "$COL_TIMESTAMP ASC"
        )
        
        val result = mutableListOf<HistoryPoint>()
        with(cursor) {
            while (moveToNext()) {
                result.add(
                    HistoryPoint(
                        timestamp = getLong(getColumnIndexOrThrow(COL_TIMESTAMP)),
                        level = getInt(getColumnIndexOrThrow(COL_LEVEL)),
                        isCharging = getInt(getColumnIndexOrThrow(COL_IS_CHARGING)) == 1,
                        powerMw = getColumnIndexOrThrow(COL_POWER_MW).let { if (isNull(it)) null else getInt(it) }
                    )
                )
            }
            close()
        }
        db.close()
        return result
    }
}

data class HistoryPoint(val timestamp: Long, val level: Int, val isCharging: Boolean, val powerMw: Int? = null)

/** CSV with an ISO-8601 UTC time column so it opens cleanly in any spreadsheet. */
fun List<HistoryPoint>.toCsv(): String = buildString {
    append("time_utc,level_percent,charging,power_w\n")
    val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
    for (p in this@toCsv) {
        append(format.format(java.util.Date(p.timestamp))).append(',')
        append(p.level).append(',')
        append(if (p.isCharging) "yes" else "no").append(',')
        p.powerMw?.let { append(String.format(java.util.Locale.US, "%.2f", it / 1000f)) }
        append('\n')
    }
}
