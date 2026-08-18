package com.rahmatsobrian.sirohaequ.audio

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import com.rahmatsobrian.sirohaequ.logging.AppLogger

/**
 * Describes what the currently-active audio session actually supports.
 *
 * This exists because the spec explicitly forbids pretending a feature works
 * when the platform/hardware can't do it: every UI control that maps to an
 * AudioEffect must be gated on a capability flag here, shown as disabled with
 * a reason, and must never crash the session if effect creation throws.
 */
data class AudioCapabilities(
    val equalizerAvailable: Boolean,
    val equalizerBandCount: Int,
    val equalizerFreqRangeHz: List<IntRange>,
    val bassBoostAvailable: Boolean,
    val bassBoostStrengthSupported: Boolean,
    val loudnessEnhancerAvailable: Boolean,
    val presetReverbAvailable: Boolean,
    val unavailableReasons: Map<String, String>
)

object AudioCapabilityProbe {

    /**
     * Attempts to instantiate each AudioEffect against [audioSessionId] to see
     * what the platform actually grants, then immediately releases them.
     * Never throws — any failure downgrades that specific capability instead
     * of propagating, per the "detect, disable, explain, don't crash" rule.
     */
    fun probe(audioSessionId: Int): AudioCapabilities {
        var eqAvailable = false
        var bandCount = 0
        val freqRanges = mutableListOf<IntRange>()
        val reasons = mutableMapOf<String, String>()

        try {
            val eq = Equalizer(0, audioSessionId)
            eqAvailable = true
            bandCount = eq.numberOfBands.toInt()
            for (b in 0 until bandCount) {
                val range = eq.getBandFreqRange(b.toShort())
                freqRanges += (range[0] / 1000) until (range[1] / 1000 + 1)
            }
            eq.release()
        } catch (e: RuntimeException) {
            reasons["equalizer"] = "Audio backend tidak mengekspos Equalizer effect (${e.javaClass.simpleName})"
            AppLogger.log("AudioCapabilityProbe", "Equalizer unavailable: ${e.message}")
        }

        var bassAvailable = false
        var bassStrength = false
        try {
            val bass = BassBoost(0, audioSessionId)
            bassAvailable = true
            bassStrength = bass.strengthSupported
            bass.release()
        } catch (e: RuntimeException) {
            reasons["bassBoost"] = "BassBoost effect tidak tersedia pada perangkat/backend ini"
            AppLogger.log("AudioCapabilityProbe", "BassBoost unavailable: ${e.message}")
        }

        var loudnessAvailable = false
        try {
            val le = LoudnessEnhancer(audioSessionId)
            loudnessAvailable = true
            le.release()
        } catch (e: RuntimeException) {
            reasons["loudnessEnhancer"] = "LoudnessEnhancer effect tidak tersedia"
            AppLogger.log("AudioCapabilityProbe", "LoudnessEnhancer unavailable: ${e.message}")
        }

        var reverbAvailable = false
        try {
            val pr = PresetReverb(0, audioSessionId)
            reverbAvailable = true
            pr.release()
        } catch (e: RuntimeException) {
            reasons["presetReverb"] = "PresetReverb effect tidak tersedia"
        }

        return AudioCapabilities(
            equalizerAvailable = eqAvailable,
            equalizerBandCount = bandCount,
            equalizerFreqRangeHz = freqRanges,
            bassBoostAvailable = bassAvailable,
            bassBoostStrengthSupported = bassStrength,
            loudnessEnhancerAvailable = loudnessAvailable,
            presetReverbAvailable = reverbAvailable,
            unavailableReasons = reasons
        )
    }

    /** Checks whether the AudioEffect base API itself grants access on this device. */
    fun hasSystemAudioEffectsAccess(): Boolean = try {
        AudioEffect.queryEffects()
        true
    } catch (e: RuntimeException) {
        false
    }
}
