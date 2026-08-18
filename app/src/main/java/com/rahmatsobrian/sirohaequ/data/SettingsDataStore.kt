package com.rahmatsobrian.sirohaequ.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "siroha_equ_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class LogLevel { DEBUG, INFO, WARN, ERROR }

/**
 * Thin typed wrapper over Preferences DataStore. Survives app restart / device
 * reboot / app update by construction (DataStore backs onto app-private disk
 * storage, not cache).
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val DEFAULT_PRESET_ID = stringPreferencesKey("default_preset_id")
        val PREAMP_DB = floatPreferencesKey("preamp_db")
        val LIMITER_ENABLED = booleanPreferencesKey("limiter_enabled")
        val CROSSFEED_PERCENT = intPreferencesKey("crossfeed_percent")
        val AUTO_PROFILE = booleanPreferencesKey("auto_profile")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LOG_LEVEL = stringPreferencesKey("log_level")
        val DEBUG_MODE = booleanPreferencesKey("debug_mode")
        val PERFORMANCE_MODE = stringPreferencesKey("performance_mode") // "battery_saver" | "balanced" | "performance"
    }

    val eqEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.EQ_ENABLED] ?: true }
    val defaultPresetId: Flow<String> = context.dataStore.data.map { it[Keys.DEFAULT_PRESET_ID] ?: "builtin_flat" }
    val preampDb: Flow<Float> = context.dataStore.data.map { it[Keys.PREAMP_DB] ?: 0f }
    val limiterEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.LIMITER_ENABLED] ?: true }
    val crossfeedPercent: Flow<Int> = context.dataStore.data.map { it[Keys.CROSSFEED_PERCENT] ?: 0 }
    val autoProfile: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_PROFILE] ?: true }
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map {
        runCatching { ThemeMode.valueOf(it[Keys.THEME_MODE] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM)
    }
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: true }
    val logLevel: Flow<LogLevel> = context.dataStore.data.map {
        runCatching { LogLevel.valueOf(it[Keys.LOG_LEVEL] ?: "INFO") }.getOrDefault(LogLevel.INFO)
    }
    val debugMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.DEBUG_MODE] ?: false }
    val performanceMode: Flow<String> = context.dataStore.data.map { it[Keys.PERFORMANCE_MODE] ?: "balanced" }

    suspend fun setEqEnabled(v: Boolean) = context.dataStore.edit { it[Keys.EQ_ENABLED] = v }
    suspend fun setDefaultPresetId(v: String) = context.dataStore.edit { it[Keys.DEFAULT_PRESET_ID] = v }
    suspend fun setPreampDb(v: Float) = context.dataStore.edit { it[Keys.PREAMP_DB] = v }
    suspend fun setLimiterEnabled(v: Boolean) = context.dataStore.edit { it[Keys.LIMITER_ENABLED] = v }
    suspend fun setCrossfeedPercent(v: Int) = context.dataStore.edit { it[Keys.CROSSFEED_PERCENT] = v }
    suspend fun setAutoProfile(v: Boolean) = context.dataStore.edit { it[Keys.AUTO_PROFILE] = v }
    suspend fun setThemeMode(v: ThemeMode) = context.dataStore.edit { it[Keys.THEME_MODE] = v.name }
    suspend fun setDynamicColor(v: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = v }
    suspend fun setLogLevel(v: LogLevel) = context.dataStore.edit { it[Keys.LOG_LEVEL] = v.name }
    suspend fun setDebugMode(v: Boolean) = context.dataStore.edit { it[Keys.DEBUG_MODE] = v }
    suspend fun setPerformanceMode(v: String) = context.dataStore.edit { it[Keys.PERFORMANCE_MODE] = v }
}
