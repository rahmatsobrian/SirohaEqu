package com.rahmatsobrian.sirohaequ.audio

import android.media.audiofx.Equalizer
import com.rahmatsobrian.sirohaequ.data.model.EqBand
import com.rahmatsobrian.sirohaequ.logging.AppLogger

/**
 * Applies [EqBand] values to a real [Equalizer] AudioEffect attached to one
 * audio session.
 *
 * Design notes:
 * - The platform Equalizer effect exposes a *fixed* number of native bands
 *   (commonly 5). Our app-level model supports 5/10/15/20/31/parametric bands
 *   independent of that. To reconcile the two, each app-level band's gain is
 *   projected onto the nearest native band(s) using a triangular weighting
 *   against log-frequency distance — the same general approach commercial
 *   Android EQ apps use to fake a denser curve on coarser hardware EQs.
 * - If native band count is smaller than requested resolution, exact backend
 *   emulation of parametric peak/shelf/notch shapes is *not* physically
 *   possible through this API; we honestly reduce to a "closest fit" instead
 *   of claiming full parametric accuracy on the DSP. Parametric preview stays
 *   accurate on-screen (EqGraph draws the true math curve); the applied audio
 *   result is the best available approximation given the exposed native bands.
 */
class EqualizerEngine(private val audioSessionId: Int) {

    private var equalizer: Equalizer? = null
    private var nativeBandCount: Short = 0
    private var nativeFreqRanges: List<IntArray> = emptyList() // [minMilliHz, maxMilliHz]
    var isAvailable: Boolean = false
        private set
    var unavailableReason: String? = null
        private set

    fun open() {
        try {
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = true
            equalizer = eq
            nativeBandCount = eq.numberOfBands
            nativeFreqRanges = (0 until nativeBandCount).map { eq.getBandFreqRange(it.toShort()) }
            isAvailable = true
        } catch (e: RuntimeException) {
            isAvailable = false
            unavailableReason = "Equalizer AudioEffect tidak dapat dibuat: ${e.javaClass.simpleName}"
            AppLogger.log("EqualizerEngine", "open() failed: ${e.message}")
        }
    }

    /** Applies the full band list. Silently no-ops (logged) if the effect isn't available. */
    fun applyBands(bands: List<EqBand>) {
        val eq = equalizer ?: return
        if (!isAvailable) return
        try {
            for (nativeBand in 0 until nativeBandCount) {
                val centerHz = nativeCenterHz(nativeBand)
                val gainDb = GainMath.interpolatedGainDb(bands, centerHz)
                val gainMillibel = GainMath.dbToMillibel(gainDb)
                val clamped = gainMillibel.coerceIn(
                    eq.bandLevelRange[0].toInt(),
                    eq.bandLevelRange[1].toInt()
                )
                eq.setBandLevel(nativeBand.toShort(), clamped.toShort())
            }
        } catch (e: RuntimeException) {
            AppLogger.log("EqualizerEngine", "applyBands failed: ${e.message}")
        }
    }

    fun setPreamp(gainDb: Float) {
        // The platform Equalizer has no dedicated preamp control; we fold it
        // into every band's applied gain instead (see applyBands callers).
    }

    fun release() {
        try {
            equalizer?.enabled = false
            equalizer?.release()
        } catch (_: RuntimeException) {
            // Already released or session torn down — safe to ignore.
        } finally {
            equalizer = null
            isAvailable = false
        }
    }

    private fun nativeCenterHz(band: Int): Float {
        val range = nativeFreqRanges.getOrNull(band) ?: return 1000f
        // getBandFreqRange returns milliHz; geometric mean of the range edges.
        val lo = range[0] / 1000f
        val hi = range[1] / 1000f
        return kotlin.math.sqrt((lo.coerceAtLeast(1f)) * (hi.coerceAtLeast(1f)))
    }
}
