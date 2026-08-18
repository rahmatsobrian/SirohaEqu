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
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true
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
        settingsRepo.dynamicColor
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
            themeMode = values[8] as ThemeMode,
            dynamicColor = values[9] as Boolean
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
                val autoEnabled = uiState.value.eqEnabled // cheap gate; full check below
                val profiles = uiState.value.deviceProfiles
                val match = AudioDeviceManager.matchProfile(device, profiles)
                if (match != null && match.isAutoApply) {
                    presetRepo.get(match.presetId)?.let { applyPreset(it) }
                    AppLogger.log("EqualizerViewModel", "Auto-applied profile '${match.displayName}' for device '${device.name}'")
                }
            }
        }
    }

    /** Called once we have a real audio session id (e.g. from a control-session broadcast). */
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
        val updated = current.copy(bands = updatedBands)
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
                createdAtEpochMs = System.currentTimeMillis()
            )
            presetRepo.save(newPreset)
        }
    }

    fun deletePreset(id: String) {
        viewModelScope.launch { presetRepo.delete(id) }
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

    override fun onCleared() {
        super.onCleared()
        deviceManager.stopWatching()
        audioEngine.release()
    }
}
