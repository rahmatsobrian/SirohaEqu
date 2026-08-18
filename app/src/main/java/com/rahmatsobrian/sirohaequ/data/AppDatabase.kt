package com.rahmatsobrian.sirohaequ.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rahmatsobrian.sirohaequ.data.model.BuiltInPresets
import com.rahmatsobrian.sirohaequ.data.model.DeviceProfile
import com.rahmatsobrian.sirohaequ.data.model.Preset
import com.rahmatsobrian.sirohaequ.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isBuiltIn: Boolean,
    val json: String,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "device_profiles")
data class DeviceProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val json: String
)

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY isBuiltIn DESC, name ASC")
    fun observeAll(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PresetEntity)

    @Query("DELETE FROM presets WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteUserPreset(id: String)

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun count(): Int
}

@Dao
interface DeviceProfileDao {
    @Query("SELECT * FROM device_profiles")
    fun observeAll(): Flow<List<DeviceProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DeviceProfileEntity)

    @Query("DELETE FROM device_profiles WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(
    entities = [PresetEntity::class, DeviceProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun deviceProfileDao(): DeviceProfileDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "siroha_equ.db")
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }
}

class PresetRepository(context: Context) {
    private val dao = AppDatabase.get(context).presetDao()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val presets: Flow<List<Preset>> = dao.observeAll().map { list ->
        list.mapNotNull { entity -> decode(entity.json) }
    }

    suspend fun ensureSeeded() {
        if (dao.count() == 0) {
            BuiltInPresets.all().forEach { save(it) }
        }
    }

    suspend fun save(preset: Preset) {
        val stamped = preset.copy(updatedAtEpochMs = System.currentTimeMillis())
        dao.upsert(
            PresetEntity(
                id = stamped.id,
                name = stamped.name,
                isBuiltIn = stamped.isBuiltIn,
                json = json.encodeToString(Preset.serializer(), stamped),
                updatedAtEpochMs = stamped.updatedAtEpochMs
            )
        )
    }

    suspend fun delete(id: String) = dao.deleteUserPreset(id)

    suspend fun get(id: String): Preset? = dao.getById(id)?.let { decode(it.json) }

    /** Export a single preset as pretty JSON, forward-compatible via schemaVersion. */
    suspend fun exportJson(id: String): String? = dao.getById(id)?.json?.let {
        // Re-serialize pretty for human-readable export files.
        decode(it)?.let { preset -> Json { prettyPrint = true }.encodeToString(Preset.serializer(), preset) }
    }

    /** Import from JSON text; migrates older schemaVersions if needed. */
    suspend fun importJson(text: String): Result<Preset> = try {
        val preset = decode(text) ?: throw IllegalArgumentException("JSON tidak valid")
        val migrated = migrate(preset)
        save(migrated)
        Result.success(migrated)
    } catch (e: Exception) {
        AppLogger.logError("PresetRepository", "importJson failed", e)
        Result.failure(e)
    }

    private fun migrate(preset: Preset): Preset =
        // Placeholder for future schema migrations; currently a no-op since
        // PRESET_SCHEMA_VERSION == 1 is the only version that has existed.
        preset

    private fun decode(text: String): Preset? = try {
        json.decodeFromString(Preset.serializer(), text)
    } catch (e: Exception) {
        AppLogger.logError("PresetRepository", "decode failed", e)
        null
    }
}

class DeviceProfileRepository(context: Context) {
    private val dao = AppDatabase.get(context).deviceProfileDao()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val profiles: Flow<List<DeviceProfile>> = dao.observeAll().map { list ->
        list.mapNotNull { entity -> decode(entity.json) }
    }

    suspend fun save(profile: DeviceProfile) {
        dao.upsert(
            DeviceProfileEntity(
                id = profile.id,
                displayName = profile.displayName,
                json = json.encodeToString(DeviceProfile.serializer(), profile)
            )
        )
    }

    suspend fun delete(id: String) = dao.delete(id)

    private fun decode(text: String): DeviceProfile? = try {
        json.decodeFromString(DeviceProfile.serializer(), text)
    } catch (e: Exception) {
        AppLogger.logError("DeviceProfileRepository", "decode failed", e)
        null
    }
}
