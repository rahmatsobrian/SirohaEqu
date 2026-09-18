package com.rahmatsobrian.sirohaequ.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rahmatsobrian.sirohaequ.audio.AudioCapabilities
import com.rahmatsobrian.sirohaequ.audio.AudioDeviceManager
import com.rahmatsobrian.sirohaequ.audio.AudioEngine
import com.rahmatsobrian.sirohaequ.audio.EngineState
import com.rahmatsobrian.sirohaequ.data.DeviceProfileRepository
import com.rahmatsobrian.sirohaequ.data.PresetRepository
import com.rahmatsobrian.sirohaequ.data.SettingsRepository
import com.rahmatsobrian.sirohaequ.data.ThemeMode
import com.rahmatsobrian.sirohaequ.data.model.DeviceProfile
import com.rahmatsobrian.sirohaequ.data.model.EqBand
import com.rahmatsobrian.sirohaequ.data.model.Preset
import com.rahmatsobrian.sirohaequ.data.model.ProcessingChain
import com.rahmatsobrian.sirohaequ.data.model.flatPreset
import com.rahmatsobrian.sirohaequ.logging.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EqualizerUiState(
    val engineState: EngineState = EngineState.IDLE,
    val capabilities: AudioCapabilities? = null,
    val lastError: String? = null,
    val eqEnabled: Boolean = true,
    val activePreset: Preset = flatPreset(),
    val presets: List<Preset> = emptyList(),
    val deviceProfiles: List<DeviceProfile> = emptyList(),
    val activeDeviceName: String? = null,
    val activeDeviceType: Int? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val preampDb: Float = 0f,
    val limiterEnabled: Boolean = true,
    val crossfeedPercent: Int = 0,
    val autoProfile: Boolean = true,
    val performanceMode: String = "balanced"
)

class EqualizerViewModel(application: Application) : AndroidViewModel(application) {

    private val presetRepo = PresetRepository(application)
    private val deviceProfileRepo = DeviceProfileRepository(application)
    private val settingsRepo = SettingsRepository(application)
    private val audioEngine = AudioEngine(application)
    private val deviceManager = AudioDeviceManager(application)

    private val _activePreset = MutableStateFlow(flatPreset())

    val uiState: StateFlow<EqualizerUiState> = combine(
        audioEngine.state,
        audioEngine.capabilities,
        audioEngine.lastError,
        settingsRepo.eqEnabled,
        _activePreset,
        presetRepo.presets,
        deviceProfileRepo.profiles,
        deviceManager.activeDevice,
        settingsRepo.themeMode,
        settingsRepo.dynamicColor,
        settingsRepo.preampDb,
        settingsRepo.limiterEnabled,
        settingsRepo.crossfeedPercent,
        settingsRepo.autoProfile,
        settingsRepo.performanceMode
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        EqualizerUiState(
            engineState = values[0] as EngineState,
            capabilities = values[1] as AudioCapabilities?,
            lastError = values[2] as String?,
            eqEnabled = values[3] as Boolean,
            activePreset = values[4] as Preset,
            presets = values[5] as List<Preset>,
            deviceProfiles = values[6] as List<DeviceProfile>,
            activeDeviceName = (values[7] as com.rahmatsobrian.sirohaequ.audio.ActiveDeviceInfo?)?.name,
            activeDeviceType = (values[7] as com.rahmatsobrian.sirohaequ.audio.ActiveDeviceInfo?)?.androidType,
            themeMode = values[8] as ThemeMode,
            dynamicColor = values[9] as Boolean,
            preampDb = values[10] as Float,
            limiterEnabled = values[11] as Boolean,
            crossfeedPercent = values[12] as Int,
            autoProfile = values[13] as Boolean,
            performanceMode = values[14] as String
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EqualizerUiState())

    init {
        viewModelScope.launch {
            try {
                presetRepo.ensureSeeded()
            } catch (e: Exception) {
                AppLogger.logError("EqualizerViewModel", "ensureSeeded failed", e)
            }
        }
        deviceManager.startWatching()
        observeAutoProfile()
    }

    private fun observeAutoProfile() {
        viewModelScope.launch {
            deviceManager.activeDevice.collect { device ->
                if (device == null) return@collect
                val profiles = uiState.value.deviceProfiles
                val match = AudioDeviceManager.matchProfile(device, profiles)
                if (match != null && match.isAutoApply) {
                    presetRepo.get(match.presetId)?.let { applyPreset(it) }
                    AppLogger.log("EqualizerViewModel", "Auto-applied profile '${match.displayName}' for device '${device.name}'")
                }
            }
        }
    }

    fun attachToAudioSession(sessionId: Int) {
        audioEngine.attachToSession(sessionId)
        audioEngine.applyPreset(_activePreset.value)
    }

    fun applyPreset(preset: Preset) {
        _activePreset.value = preset
        audioEngine.applyPreset(preset)
        viewModelScope.launch {
            settingsRepo.setDefaultPresetId(preset.id)
        }
    }

    fun updateBand(bandId: Int, newGainDb: Float) {
        val current = _activePreset.value
        val updatedBands = current.bands.map {
            if (it.id == bandId) it.copy(gainDb = newGainDb.coerceIn(-24f, 24f)) else it
        }
        val updated = current.copy(bands = updatedBands, isBuiltIn = false)
        _activePreset.value = updated
        audioEngine.applyPreset(updated)
    }

    fun updateBandQ(bandId: Int, newQ: Float) {
        val current = _activePreset.value
        val updatedBands = current.bands.map {
            if (it.id == bandId) it.copy(qFactor = newQ.coerceIn(0.1f, 10f)) else it
        }
        val updated = current.copy(bands = updatedBands, isBuiltIn = false)
        _activePreset.value = updated
        audioEngine.applyPreset(updated)
    }

    fun updateProcessingChain(chain: ProcessingChain) {
        val current = _activePreset.value
        val updated = current.copy(chain = chain, isBuiltIn = false)
        _activePreset.value = updated
        audioEngine.applyPreset(updated)
    }

    fun setEqEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setEqEnabled(enabled) }
        if (!enabled) {
            audioEngine.applyPreset(flatPreset(_activePreset.value.id))
        } else {
            audioEngine.applyPreset(_activePreset.value)
        }
    }

    fun savePresetAs(name: String) {
        viewModelScope.launch {
            val newPreset = _activePreset.value.copy(
                id = "user_${System.currentTimeMillis()}",
                name = name,
                isBuiltIn = false,
                createdAtEpochMs = System.currentTimeMillis(),
                updatedAtEpochMs = System.currentTimeMillis()
            )
            presetRepo.save(newPreset)
        }
    }

    fun deletePreset(id: String) {
        viewModelScope.launch { presetRepo.delete(id) }
    }

    fun duplicatePreset(source: Preset, newName: String) {
        viewModelScope.launch {
            val dup = source.copy(
                id = "user_${System.currentTimeMillis()}",
                name = newName,
                isBuiltIn = false,
                createdAtEpochMs = System.currentTimeMillis(),
                updatedAtEpochMs = System.currentTimeMillis()
            )
            presetRepo.save(dup)
        }
    }

    fun exportPresetJson(id: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val json = presetRepo.exportJson(id)
            onResult(json)
        }
    }

    fun importPresetJson(json: String, onResult: (Result<Preset>) -> Unit) {
        viewModelScope.launch {
            val result = presetRepo.importJson(json)
            onResult(result)
        }
    }

    fun saveDeviceProfile(profile: DeviceProfile) {
        viewModelScope.launch { deviceProfileRepo.save(profile) }
    }

    fun deleteDeviceProfile(id: String) {
        viewModelScope.launch { deviceProfileRepo.delete(id) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setDynamicColor(enabled) }
    }

    fun setPreampDb(db: Float) {
        viewModelScope.launch { settingsRepo.setPreampDb(db) }
        val current = _activePreset.value
        val updated = current.copy(
            chain = current.chain.copy(preampDb = db),
            isBuiltIn = false
        )
        _activePreset.value = updated
        audioEngine.applyPreset(updated)
    }

    fun setLimiterEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setLimiterEnabled(enabled) }
        val current = _activePreset.value
        val updated = current.copy(
            chain = current.chain.copy(limiterEnabled = enabled),
            isBuiltIn = false
        )
        _activePreset.value = updated
        audioEngine.applyPreset(updated)
    }

    fun setCrossfeedPercent(percent: Int) {
        viewModelScope.launch { settingsRepo.setCrossfeedPercent(percent) }
        val current = _activePreset.value
        val updated = current.copy(
            chain = current.chain.copy(crossfeedPercent = percent),
            isBuiltIn = false
        )
        _activePreset.value = updated
        audioEngine.applyPreset(updated)
    }

    fun setAutoProfile(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setAutoProfile(enabled) }
    }

    fun setPerformanceMode(mode: String) {
        viewModelScope.launch { settingsRepo.setPerformanceMode(mode) }
    }

    fun setBalance(balance: Float) {
        val current = _activePreset.value
        val updated = current.copy(
            chain = current.chain.copy(balance = balance.coerceIn(-1f, 1f)),
            isBuiltIn = false
        )
        _activePreset.value = updated
        audioEngine.applyPreset(updated)
    }

    fun setMono(enabled: Boolean) {
        val current = _activePreset.value
        val updated = current.copy(
            chain = current.chain.copy(monoEnabled = enabled),
            isBuiltIn = false
        )
        _activePreset.value = updated
        audioEngine.applyPreset(updated)
    }

    fun setStereoWidth(percent: Int) {
        val current = _activePreset.value
        val updated = current.copy(
            chain = current.chain.copy(stereoWidthPercent = percent.coerceIn(0, 200)),
            isBuiltIn = false
        )
        _activePreset.value = updated
        audioEngine.applyPreset(updated)
    }

    fun setBassBoostDb(db: Float) {
        val current = _activePreset.value
        val updated = current.copy(
            chain = current.chain.copy(bassBoostDb = db.coerceIn(0f, 24f)),
            isBuiltIn = false
        )
        _activePreset.value = updated
        audioEngine.applyPreset(updated)
    }

    fun setSubBassBoostDb(db: Float) {
        val current = _activePreset.value
        val updated = current.copy(
            chain = current.chain.copy(subBassBoostDb = db.coerceIn(0f, 24f)),
            isBuiltIn = false
        )
        _activePreset.value = updated
        audioEngine.applyPreset(updated)
    }

    fun setTrebleBoostDb(db: Float) {
        val current = _activePreset.value
        val updated = current.copy(
            chain = current.chain.copy(trebleBoostDb = db.coerceIn(0f, 24f)),
            isBuiltIn = false
        )
        _activePreset.value = updated
        audioEngine.applyPreset(updated)
    }

    fun setAirBoostDb(db: Float) {
        val current = _activePreset.value
        val updated = current.copy(
            chain = current.chain.copy(airBoostDb = db.coerceIn(0f, 24f)),
            isBuiltIn = false
        )
        _activePreset.value = updated
        audioEngine.applyPreset(updated)
    }

    fun setVolumeNormalization(enabled: Boolean) {
        val current = _activePreset.value
        val updated = current.copy(
            chain = current.chain.copy(volumeNormalizationEnabled = enabled),
            isBuiltIn = false
        )
        _activePreset.value = updated
        audioEngine.applyPreset(updated)
    }

    fun setCompressor(enabled: Boolean, thresholdDb: Float = -18f, ratio: Float = 2f) {
        val current = _activePreset.value
        val updated = current.copy(
            chain = current.chain.copy(
                compressorEnabled = enabled,
                compressorThresholdDb = thresholdDb,
                compressorRatio = ratio
            ),
            isBuiltIn = false
        )
        _activePreset.value = updated
        audioEngine.applyPreset(updated)
    }

    override fun onCleared() {
        super.onCleared()
        deviceManager.stopWatching()
        audioEngine.release()
    }
}
