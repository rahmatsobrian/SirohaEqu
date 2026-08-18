package com.rahmatsobrian.sirohaequ.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import com.rahmatsobrian.sirohaequ.logging.AppLogger

/**
 * Wraps [BassBoost] and [LoudnessEnhancer]. Treble/air boost do not have a
 * dedicated platform AudioEffect, so they are realized as extra high-shelf
 * bias folded into [EqualizerEngine]'s applied bands by the caller
 * (AudioEngine), not here — this class only owns the effects that map
 * 1:1 to a real platform API.
 */
class BassTrebleEngine(private val audioSessionId: Int) {

    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    var bassBoostAvailable = false
        private set
    var bassStrengthSupported = false
        private set
    var loudnessAvailable = false
        private set

    fun open() {
        try {
            val bb = BassBoost(0, audioSessionId)
            bassBoostAvailable = true
            bassStrengthSupported = bb.strengthSupported
            bassBoost = bb
        } catch (e: RuntimeException) {
            bassBoostAvailable = false
            AppLogger.log("BassTrebleEngine", "BassBoost open failed: ${e.message}")
        }

        try {
            val le = LoudnessEnhancer(audioSessionId)
            loudnessAvailable = true
            loudnessEnhancer = le
        } catch (e: RuntimeException) {
            loudnessAvailable = false
            AppLogger.log("BassTrebleEngine", "LoudnessEnhancer open failed: ${e.message}")
        }
    }

    /** [strengthPercent] 0..1000 per platform BassBoost convention. */
    fun setBassStrength(strengthPercent: Short) {
        val bb = bassBoost ?: return
        if (!bassBoostAvailable || !bassStrengthSupported) return
        try {
            bb.enabled = strengthPercent > 0
            bb.setStrength(strengthPercent)
        } catch (e: RuntimeException) {
            AppLogger.log("BassTrebleEngine", "setBassStrength failed: ${e.message}")
        }
    }

    fun setLoudnessGainMillibel(gainMb: Float) {
        val le = loudnessEnhancer ?: return
        if (!loudnessAvailable) return
        try {
            le.enabled = gainMb > 0f
            le.setTargetGain(gainMb.toInt())
        } catch (e: RuntimeException) {
            AppLogger.log("BassTrebleEngine", "setLoudnessGain failed: ${e.message}")
        }
    }

    fun release() {
        try { bassBoost?.release() } catch (_: RuntimeException) {}
        try { loudnessEnhancer?.release() } catch (_: RuntimeException) {}
        bassBoost = null
        loudnessEnhancer = null
    }
}
