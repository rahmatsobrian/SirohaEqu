package com.rahmatsobrian.sirohaequ.audio

import com.rahmatsobrian.sirohaequ.data.model.EqBand
import kotlin.math.abs
import kotlin.math.ln

/**
 * Pure math extracted so it can be unit-tested on the plain JVM without any
 * Android framework dependency (no Robolectric/instrumentation needed).
 * Both [EqualizerEngine] (applies to real hardware) and EqGraph (draws the
 * on-screen preview curve) call this so what the user sees always matches
 * what gets applied.
 */
object GainMath {

    /** Triangular log-frequency weighted interpolation of band gains at [targetHz]. */
    fun interpolatedGainDb(bands: List<EqBand>, targetHz: Float): Float {
        if (bands.isEmpty()) return 0f
        val logTarget = ln(targetHz.coerceAtLeast(1f).toDouble())
        var weightedSum = 0.0
        var weightTotal = 0.0
        for (band in bands) {
            if (!band.enabled) continue
            val logBand = ln(band.frequencyHz.coerceAtLeast(1f).toDouble())
            val distance = abs(logBand - logTarget)
            val weight = 1.0 / (1.0 + distance * distance * 4.0)
            weightedSum += weight * band.gainDb
            weightTotal += weight
        }
        return if (weightTotal > 0) (weightedSum / weightTotal).toFloat() else 0f
    }

    fun dbToMillibel(db: Float): Int = (db * 100).toInt()

    fun millibelToDb(mb: Int): Float = mb / 100f
}
