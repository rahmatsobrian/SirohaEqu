package com.rahmatsobrian.sirohaequ.data.model

import kotlinx.serialization.Serializable

/** Bump when the on-disk/export JSON shape changes; PresetRepository migrates on read. */
const val PRESET_SCHEMA_VERSION = 1

@Serializable
data class ProcessingChain(
    val preampDb: Float = 0f,
    val bassBoostDb: Float = 0f,
    val subBassBoostDb: Float = 0f,
    val bassFrequencyHz: Float = 80f,
    val bassQ: Float = 0.9f,
    val trebleBoostDb: Float = 0f,
    val airBoostDb: Float = 0f,
    val trebleFrequencyHz: Float = 8000f,
    val trebleQ: Float = 0.9f,
    val loudnessEnabled: Boolean = false,
    val balance: Float = 0f, // -1 (full left) .. +1 (full right)
    val monoEnabled: Boolean = false,
    val stereoWidthPercent: Int = 100, // 0..200
    val crossfeedPercent: Int = 0, // 0..100, headphone-only
    val compressorEnabled: Boolean = false,
    val compressorThresholdDb: Float = -18f,
    val compressorRatio: Float = 2f,
    val limiterEnabled: Boolean = true,
    val limiterCeilingDb: Float = -1f,
    val outputGainDb: Float = 0f,
    val volumeNormalizationEnabled: Boolean = false
)

@Serializable
data class Preset(
    val schemaVersion: Int = PRESET_SCHEMA_VERSION,
    val id: String,
    val name: String,
    val isBuiltIn: Boolean = false,
    val bandCount: Int = 10,
    val bands: List<EqBand>,
    val chain: ProcessingChain = ProcessingChain(),
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

/** Flat-response, zero-processing preset — the safe fallback/default state. */
fun flatPreset(id: String = "builtin_flat"): Preset {
    val freqs = StandardFrequencies.forBandCount(10)
    return Preset(
        id = id,
        name = "Flat",
        isBuiltIn = true,
        bandCount = 10,
        bands = freqs.mapIndexed { i, f -> EqBand(id = i, frequencyHz = f, gainDb = 0f) }
    )
}

/** The full built-in preset library described in the spec (19 curated presets). */
object BuiltInPresets {

    private fun bands(count: Int, gains: List<Float>): List<EqBand> {
        val freqs = StandardFrequencies.forBandCount(count)
        return freqs.mapIndexed { i, f ->
            EqBand(id = i, frequencyHz = f, gainDb = gains.getOrElse(i) { 0f })
        }
    }

    // 10-band gain curves, low -> high frequency.
    private val curves: Map<String, List<Float>> = mapOf(
        "Flat" to List(10) { 0f },
        "Bass Boost" to listOf(6f, 5f, 3f, 1f, 0f, 0f, 0f, 0f, 0f, 0f),
        "Deep Bass" to listOf(8f, 7f, 5f, 2f, 0f, -1f, -1f, 0f, 0f, 0f),
        "Vocal" to listOf(-2f, -1f, 0f, 2f, 4f, 4f, 2f, 0f, -1f, -1f),
        "Clear Vocal" to listOf(-3f, -2f, -1f, 1f, 3f, 5f, 4f, 2f, 0f, -1f),
        "Podcast" to listOf(-4f, -2f, 0f, 2f, 4f, 4f, 2f, 0f, -2f, -3f),
        "Rock" to listOf(4f, 3f, 1f, 0f, -1f, 0f, 1f, 2f, 3f, 3f),
        "Pop" to listOf(1f, 2f, 1f, 0f, -1f, -1f, 0f, 1f, 2f, 2f),
        "Classical" to listOf(2f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 2f, 3f),
        "Jazz" to listOf(3f, 2f, 1f, 1f, 0f, 0f, 1f, 1f, 2f, 2f),
        "EDM" to listOf(6f, 5f, 2f, 0f, -1f, 0f, 1f, 2f, 4f, 5f),
        "Gaming" to listOf(3f, 2f, 0f, 1f, 2f, 2f, 1f, 2f, 3f, 3f),
        "Movie" to listOf(4f, 3f, 1f, 0f, 1f, 2f, 1f, 1f, 2f, 3f),
        "Acoustic" to listOf(2f, 1f, 0f, 1f, 2f, 2f, 1f, 1f, 1f, 0f),
        "Treble Boost" to listOf(0f, 0f, 0f, 0f, 0f, 1f, 3f, 5f, 6f, 6f),
        "Warm" to listOf(3f, 2f, 1f, 1f, 0f, -1f, -2f, -2f, -1f, -1f),
        "Bright" to listOf(-1f, -1f, 0f, 0f, 1f, 2f, 3f, 4f, 5f, 5f),
        "Neutral" to List(10) { 0f },
        "V-shaped" to listOf(5f, 4f, 2f, 0f, -2f, -2f, 0f, 2f, 4f, 5f)
    )

    fun all(): List<Preset> = curves.entries.mapIndexed { index, (name, gains) ->
        Preset(
            id = "builtin_${name.lowercase().replace(Regex("[^a-z0-9]+"), "_")}",
            name = name,
            isBuiltIn = true,
            bandCount = 10,
            bands = bands(10, gains),
            chain = ProcessingChain(
                bassBoostDb = if (name in setOf("Bass Boost", "Deep Bass", "EDM")) 4f else 0f,
                trebleBoostDb = if (name in setOf("Treble Boost", "Bright")) 3f else 0f
            )
        )
    }
}
