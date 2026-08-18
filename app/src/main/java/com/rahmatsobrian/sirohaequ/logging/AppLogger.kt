package com.rahmatsobrian.sirohaequ.logging

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Entity(tableName = "diagnostic_logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMs: Long,
    val level: String,
    val tag: String,
    val message: String,
    val appVersion: String,
    val androidVersion: String,
    val deviceManufacturer: String,
    val deviceModel: String,
    val exceptionClass: String? = null,
    val stackTrace: String? = null
)

@Dao
interface LogDao {
    @Insert
    suspend fun insert(entry: LogEntry)

    @Insert
    fun insertBlocking(entry: LogEntry)

    @Query("SELECT * FROM diagnostic_logs ORDER BY id DESC LIMIT :limit")
    suspend fun recent(limit: Int = 500): List<LogEntry>

    @Query("DELETE FROM diagnostic_logs")
    suspend fun clear()
}

@Database(entities = [LogEntry::class], version = 1, exportSchema = false)
abstract class LogDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
}

/**
 * Local-only structured logger. Never transmits anything automatically —
 * export/share is always an explicit user action from the Diagnostics screen.
 *
 * Deliberately never logs: passwords, tokens, API keys, account identifiers,
 * or any personal data. Only technical/audio-state fields are captured, per
 * spec section 12.
 */
object AppLogger {
    private lateinit var db: LogDatabase
    private lateinit var appVersion: String
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        db = Room.databaseBuilder(context.applicationContext, LogDatabase::class.java, "diagnostic_logs.db")
            .fallbackToDestructiveMigration()
            .build()
        appVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    fun log(tag: String, message: String, level: String = "INFO") {
        Log.println(levelToPriority(level), tag, message)
        if (!::db.isInitialized) return
        scope.launch {
            db.logDao().insert(buildEntry(level, tag, message, null))
        }
    }

    fun logError(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
        if (!::db.isInitialized) return
        scope.launch {
            db.logDao().insert(buildEntry("ERROR", tag, message, throwable))
        }
    }

    /** Synchronous variant for use inside the uncaught-exception handler, where
     *  the process may die before a coroutine gets scheduled. */
    fun logCrashBlocking(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
        if (!::db.isInitialized) return
        runBlocking {
            try {
                db.logDao().insertBlocking(buildEntry("CRASH", tag, message, throwable))
            } catch (_: Exception) {
                // Best effort only.
            }
        }
    }

    suspend fun recent(limit: Int = 500): List<LogEntry> =
        if (::db.isInitialized) db.logDao().recent(limit) else emptyList()

    suspend fun clear() {
        if (::db.isInitialized) db.logDao().clear()
    }

    suspend fun exportAsJson(): String {
        val entries = recent(2000)
        return Json { prettyPrint = true }.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(LogEntryDto.serializer()),
            entries.map { it.toDto() }
        )
    }

    suspend fun exportAsText(): String = buildString {
        recent(2000).forEach { e ->
            appendLine("[${e.timestampEpochMs}] ${e.level}/${e.tag}: ${e.message}")
            if (e.exceptionClass != null) appendLine("  ${e.exceptionClass}: ${e.stackTrace}")
        }
    }

    private fun buildEntry(level: String, tag: String, message: String, throwable: Throwable?): LogEntry = LogEntry(
        timestampEpochMs = System.currentTimeMillis(),
        level = level,
        tag = tag,
        message = message,
        appVersion = if (::appVersion.isInitialized) appVersion else "unknown",
        androidVersion = "API ${Build.VERSION.SDK_INT}",
        deviceManufacturer = Build.MANUFACTURER ?: "unknown",
        deviceModel = Build.MODEL ?: "unknown",
        exceptionClass = throwable?.javaClass?.name,
        stackTrace = throwable?.stackTraceToString()
    )

    private fun levelToPriority(level: String) = when (level) {
        "ERROR", "CRASH" -> Log.ERROR
        "WARN" -> Log.WARN
        "DEBUG" -> Log.DEBUG
        else -> Log.INFO
    }
}

@Serializable
data class LogEntryDto(
    val timestampEpochMs: Long,
    val level: String,
    val tag: String,
    val message: String,
    val appVersion: String,
    val androidVersion: String,
    val deviceManufacturer: String,
    val deviceModel: String,
    val exceptionClass: String? = null,
    val stackTrace: String? = null
)

private fun LogEntry.toDto() = LogEntryDto(
    timestampEpochMs, level, tag, message, appVersion, androidVersion,
    deviceManufacturer, deviceModel, exceptionClass, stackTrace
)
