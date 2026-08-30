package com.crdy.powergyan.data.local.history

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.crdy.powergyan.domain.model.BatterySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BatteryHistoryHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private val statePrefs = context.getSharedPreferences("battery_history_state", Context.MODE_PRIVATE)

    companion object {
        const val DATABASE_NAME = "battery_history.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "history"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_LEVEL = "level"
        const val COL_IS_CHARGING = "is_charging"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_NAME (" +
                    "$COL_TIMESTAMP INTEGER PRIMARY KEY," +
                    "$COL_LEVEL INTEGER," +
                    "$COL_IS_CHARGING INTEGER)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    suspend fun insertSnapshot(snapshot: BatterySnapshot) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val charging = snapshot.status == com.crdy.powergyan.domain.model.BatteryStatus.CHARGING
        val wasCharging = statePrefs.getBoolean("was_charging", false)
        if (charging && !wasCharging) {
            statePrefs.edit().putLong("last_charge_started", System.currentTimeMillis()).apply()
        }
        statePrefs.edit().putBoolean("was_charging", charging).apply()
        val values = ContentValues().apply {
            put(COL_TIMESTAMP, System.currentTimeMillis())
            put(COL_LEVEL, snapshot.percentage)
            put(COL_IS_CHARGING, if (charging) 1 else 0)
        }
        db.insert(TABLE_NAME, null, values)
        
        // Prune old data (keep last 24 hours)
        val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        db.delete(TABLE_NAME, "$COL_TIMESTAMP < ?", arrayOf(cutoff.toString()))
        db.close()
    }

    fun getLastChargeStarted(): Long? = statePrefs.getLong("last_charge_started", 0L).takeIf { it > 0L }

    suspend fun getHistory(): List<HistoryPoint> = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME, 
            arrayOf(COL_TIMESTAMP, COL_LEVEL, COL_IS_CHARGING),
            null, null, null, null, "$COL_TIMESTAMP ASC"
        )
        
        val result = mutableListOf<HistoryPoint>()
        with(cursor) {
            while (moveToNext()) {
                result.add(
                    HistoryPoint(
                        timestamp = getLong(getColumnIndexOrThrow(COL_TIMESTAMP)),
                        level = getInt(getColumnIndexOrThrow(COL_LEVEL)),
                        isCharging = getInt(getColumnIndexOrThrow(COL_IS_CHARGING)) == 1
                    )
                )
            }
            close()
        }
        db.close()
        result
    }
}

data class HistoryPoint(val timestamp: Long, val level: Int, val isCharging: Boolean)
