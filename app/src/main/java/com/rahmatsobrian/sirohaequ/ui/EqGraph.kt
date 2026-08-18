package com.rahmatsobrian.sirohaequ.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.rahmatsobrian.sirohaequ.audio.GainMath
import com.rahmatsobrian.sirohaequ.data.model.EqBand
import kotlin.math.ln

private const val MIN_FREQ = 20f
private const val MAX_FREQ = 20000f
private const val MAX_GAIN_DB = 24f

/**
 * Realtime EQ curve. Bands are draggable vertically (gain); double-tap resets
 * a band to 0 dB. Horizontal position is fixed to each band's frequency for
 * graphic-EQ modes — this view does not move frequency (that's the
 * parametric editor's job) to keep gesture handling unambiguous and fast.
 */
@Composable
fun EqGraph(
    bands: List<EqBand>,
    onBandGainChange: (bandId: Int, gainDb: Float) -> Unit,
    onBandReset: (bandId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val pointColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(bands) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val nearest = nearestBand(bands, change.position.x, size.width.toFloat())
                        if (nearest != null) {
                            val deltaDb = -dragAmount.y / size.height * (MAX_GAIN_DB * 2)
                            val newGain = (currentGain(bands, nearest) + deltaDb).coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB)
                            onBandGainChange(nearest, newGain)
                        }
                    }
                )
            }
            .pointerInput(bands) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val nearest = nearestBand(bands, offset.x, size.width.toFloat())
                        if (nearest != null) onBandReset(nearest)
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val zeroY = h / 2f

        // Horizontal gridlines at -24/-12/0/12/24 dB
        listOf(-24f, -12f, 0f, 12f, 24f).forEach { db ->
            val y = h / 2f - (db / MAX_GAIN_DB) * (h / 2f)
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }

        // Vertical gridlines at decade frequencies
        listOf(100f, 1000f, 10000f).forEach { f ->
            val x = freqToX(f, w)
            drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        }

        // EQ curve via smooth polyline sampled across the frequency axis
        if (bands.isNotEmpty()) {
            val path = androidx.compose.ui.graphics.Path()
            var first = true
            var x = 0f
            while (x <= w) {
                val freq = xToFreq(x, w)
                val gain = GainMath.interpolatedGainDb(bands, freq)
                val y = h / 2f - (gain / MAX_GAIN_DB) * (h / 2f)
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
                x += 4f
            }
            drawPath(path, color = lineColor, style = Stroke(width = 4f))
        }

        // Band handles
        bands.forEach { band ->
            val x = freqToX(band.frequencyHz, w)
            val y = h / 2f - (band.gainDb / MAX_GAIN_DB) * (h / 2f)
            drawCircle(pointColor, radius = 10f, center = Offset(x, y))
            drawCircle(Color.White, radius = 4f, center = Offset(x, y))
        }
    }
}

private fun freqToX(freq: Float, width: Float): Float {
    val logMin = ln(MIN_FREQ)
    val logMax = ln(MAX_FREQ)
    val logF = ln(freq.coerceIn(MIN_FREQ, MAX_FREQ))
    return ((logF - logMin) / (logMax - logMin)) * width
}

private fun xToFreq(x: Float, width: Float): Float {
    val logMin = ln(MIN_FREQ)
    val logMax = ln(MAX_FREQ)
    val t = (x / width).coerceIn(0f, 1f)
    return kotlin.math.exp(logMin + t * (logMax - logMin))
}

private fun nearestBand(bands: List<EqBand>, x: Float, width: Float): Int? {
    if (bands.isEmpty()) return null
    val freq = xToFreq(x, width)
    return bands.minByOrNull { kotlin.math.abs(ln(it.frequencyHz) - ln(freq)) }?.id
}

private fun currentGain(bands: List<EqBand>, bandId: Int): Float =
    bands.firstOrNull { it.id == bandId }?.gainDb ?: 0f
