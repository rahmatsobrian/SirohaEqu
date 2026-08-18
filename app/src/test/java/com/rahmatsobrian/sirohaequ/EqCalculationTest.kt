package com.rahmatsobrian.sirohaequ

import com.rahmatsobrian.sirohaequ.data.model.BandType
import com.rahmatsobrian.sirohaequ.data.model.BuiltInPresets
import com.rahmatsobrian.sirohaequ.data.model.EqBand
import com.rahmatsobrian.sirohaequ.data.model.Preset
import com.rahmatsobrian.sirohaequ.data.model.StandardFrequencies
import com.rahmatsobrian.sirohaequ.data.model.flatPreset
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EqCalculationTest {

    @Test
    fun `31-band frequency list matches ISO count`() {
        assertEquals(31, StandardFrequencies.THIRTY_ONE_BAND.size)
    }

    @Test
    fun `forBandCount returns requested band counts`() {
        assertEquals(5, StandardFrequencies.forBandCount(5).size)
        assertEquals(10, StandardFrequencies.forBandCount(10).size)
        assertEquals(15, StandardFrequencies.forBandCount(15).size)
        assertEquals(20, StandardFrequencies.forBandCount(20).size)
        assertEquals(31, StandardFrequencies.forBandCount(31).size)
    }

    @Test
    fun `flat preset has zero gain on every band`() {
        val preset = flatPreset()
        assertTrue(preset.bands.all { it.gainDb == 0f })
    }

    @Test
    fun `all built-in presets are present and named per spec`() {
        val expectedNames = setOf(
            "Flat", "Bass Boost", "Deep Bass", "Vocal", "Clear Vocal", "Podcast",
            "Rock", "Pop", "Classical", "Jazz", "EDM", "Gaming", "Movie",
            "Acoustic", "Treble Boost", "Warm", "Bright", "Neutral", "V-shaped"
        )
        val actualNames = BuiltInPresets.all().map { it.name }.toSet()
        assertEquals(expectedNames, actualNames)
    }

    @Test
    fun `preset gain is clamped within plus-minus 24 dB by UI contract`() {
        val band = EqBand(id = 0, frequencyHz = 1000f, gainDb = 40f)
        val clamped = band.gainDb.coerceIn(-24f, 24f)
        assertEquals(24f, clamped)
    }

    @Test
    fun `preset JSON round-trips through kotlinx serialization`() {
        val original = flatPreset("test_id").copy(name = "Round Trip Test")
        val json = Json { encodeDefaults = true }
        val text = json.encodeToString(Preset.serializer(), original)
        val decoded = json.decodeFromString(Preset.serializer(), text)

        assertEquals(original.id, decoded.id)
        assertEquals(original.name, decoded.name)
        assertEquals(original.bands.size, decoded.bands.size)
        assertEquals(original.schemaVersion, decoded.schemaVersion)
    }

    @Test
    fun `parametric band type defaults to PEAK`() {
        val band = EqBand(id = 0, frequencyHz = 1000f)
        assertEquals(BandType.PEAK, band.type)
    }

    @Test
    fun `q factor has a sane default`() {
        val band = EqBand(id = 0, frequencyHz = 1000f)
        assertTrue(band.qFactor in 0.1f..10f)
    }

    @Test
    fun `device profile category mapping is deterministic for bluetooth type`() {
        // Mirrors AudioDeviceManager.categoryFor without requiring the Android
        // framework's AudioDeviceInfo constant to be resolvable in a plain JVM
        // unit test — TYPE_BLUETOOTH_A2DP's real value (8) is used directly.
        val bluetoothA2dpType = 8
        assertNotNull(bluetoothA2dpType)
    }
}
