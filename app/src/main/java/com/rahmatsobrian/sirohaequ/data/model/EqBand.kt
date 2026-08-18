package com.rahmatsobrian.sirohaequ.data.model

import kotlinx.serialization.Serializable

/** Parametric band shapes. LowPass/HighPass/Notch have no useful "gain" (ignored/0). */
@Serializable
enum class BandType {
    PEAK,
    LOW_SHELF,
    HIGH_SHELF,
    LOW_PASS,
    HIGH_PASS,
    NOTCH
}

/**
 * A single EQ band, usable both by graphic-EQ presets (fixed frequency, PEAK type)
 * and by the full parametric editor (freq/gain/Q/type all editable).
 *
 * gainDb is clamped to [-24, 24] at the UI edge (see EqGraph); the model itself
 * stores whatever was set so import/export round-trips exactly.
 */
@Serializable
data class EqBand(
    val id: Int,
    val frequencyHz: Float,
    val gainDb: Float = 0f,
    val qFactor: Float = 1.41f,
    val type: BandType = BandType.PEAK,
    val enabled: Boolean = true
)

/** Standard ISO 1/3-octave centre frequencies used for the 31-band graphic mode. */
object StandardFrequencies {
    val THIRTY_ONE_BAND = listOf(
        20f, 25f, 31f, 40f, 50f, 63f, 80f, 100f, 125f, 160f, 200f, 250f, 315f,
        400f, 500f, 630f, 800f, 1000f, 1250f, 1600f, 2000f, 2500f, 3150f, 4000f,
        5000f, 6300f, 8000f, 10000f, 12500f, 16000f, 20000f
    )

    /** Even subsample of the 31-band list, spaced roughly logarithmically. */
    fun forBandCount(count: Int): List<Float> = when (count) {
        31 -> THIRTY_ONE_BAND
        20 -> THIRTY_ONE_BAND.filterIndexed { i, _ -> i % 3 != 2 }.take(20)
        15 -> THIRTY_ONE_BAND.filterIndexed { i, _ -> i % 2 == 0 }.take(15)
        10 -> listOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
        5 -> listOf(60f, 250f, 1000f, 4000f, 16000f)
        else -> THIRTY_ONE_BAND
    }
}
