package com.rahmatsobrian.sirohaequ.audio

import android.content.Context
import android.media.audiofx.AudioEffect
import com.rahmatsobrian.sirohaequ.data.model.Preset
import com.rahmatsobrian.sirohaequ.logging.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EngineState { IDLE, ACTIVE, DEGRADED, FAILED }

/**
 * Single entry point the UI/ViewModel layer talks to. Owns the lifecycle of
 * [EqualizerEngine] / [BassTrebleEngine] for whichever audio session is
 * currently targeted, and is the one place that implements the
 * "detect → disable → explain → never crash" contract from the spec.
 *
 * IMPORTANT PLATFORM LIMITATION (documented honestly, see docs/AUDIO_ENGINE.md):
 * Android does not grant normal (non-root, non-privileged) apps a public API
 * to apply audio effects to *all* system audio unconditionally. This engine
 * attaches to the audio session broadcast by cooperating player apps via
 * [AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION] (the same mechanism
 * used by mainstream Android EQ apps such as Poweramp/Wavelet), and — for the
 * app's own future in-app player, if added — its own session id directly.
 * True unconditional system-wide EQ across every app is not achievable
 * without root, and this app does not require root for its primary function.
 */
class AudioEngine(private val context: Context) {

    private var eqEngine: EqualizerEngine? = null
    private var bassTrebleEngine: BassTrebleEngine? = null
    private var currentSessionId: Int = AudioEffect.ERROR_BAD_VALUE

    private val _state = MutableStateFlow(EngineState.IDLE)
    val state: StateFlow<EngineState> = _state.asStateFlow()

    private val _capabilities = MutableStateFlow<AudioCapabilities?>(null)
    val capabilities: StateFlow<AudioCapabilities?> = _capabilities.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /** Attaches processing to a specific session id (e.g. from the control-session broadcast). */
    fun attachToSession(sessionId: Int) {
        try {
            releaseInternal()
            currentSessionId = sessionId

            val caps = AudioCapabilityProbe.probe(sessionId)
            _capabilities.value = caps

            val eq = EqualizerEngine(sessionId).also { it.open() }
            val bt = BassTrebleEngine(sessionId).also { it.open() }
            eqEngine = eq
            bassTrebleEngine = bt

            _state.value = when {
                eq.isAvailable -> EngineState.ACTIVE
                caps.bassBoostAvailable || caps.loudnessEnhancerAvailable -> EngineState.DEGRADED
                else -> EngineState.FAILED
            }
            if (!eq.isAvailable) {
                _lastError.value = eq.unavailableReason
                AppLogger.log("AudioEngine", "Equalizer unavailable on session $sessionId: ${eq.unavailableReason}", "WARN")
            }
        } catch (e: Exception) {
            // Absolute safety net: whatever goes wrong while attaching, the app
            // must recover to a safe idle state rather than crash.
            AppLogger.logError("AudioEngine", "attachToSession($sessionId) failed catastrophically", e)
            safeRecover()
        }
    }

    fun applyPreset(preset: Preset) {
        try {
            val eq = eqEngine ?: return
            val bands = preset.bands.map { band ->
                band.copy(gainDb = (band.gainDb + preset.chain.preampDb).coerceIn(-24f, 24f))
            }
            eq.applyBands(bands)

            val bt = bassTrebleEngine ?: return
            val bassStrength = ((preset.chain.bassBoostDb + preset.chain.subBassBoostDb) / 24f * 1000f)
                .coerceIn(0f, 1000f).toInt().toShort()
            bt.setBassStrength(bassStrength)

            if (preset.chain.volumeNormalizationEnabled) {
                bt.setLoudnessGainMillibel(600f)
            } else {
                bt.setLoudnessGainMillibel(0f)
            }
        } catch (e: Exception) {
            AppLogger.logError("AudioEngine", "applyPreset(${preset.id}) failed", e)
            safeRecover()
        }
    }

    fun release() {
        releaseInternal()
        _state.value = EngineState.IDLE
    }

    private fun releaseInternal() {
        try { eqEngine?.release() } catch (_: Exception) {}
        try { bassTrebleEngine?.release() } catch (_: Exception) {}
        eqEngine = null
        bassTrebleEngine = null
    }

    /** Restores a known-safe, fully-disabled state after an unexpected failure. */
    private fun safeRecover() {
        releaseInternal()
        _state.value = EngineState.FAILED
        _lastError.value = "Audio engine dipulihkan ke mode aman setelah error tak terduga. Fitur EQ dinonaktifkan sementara."
    }
}
