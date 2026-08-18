package com.rahmatsobrian.sirohaequ.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.rahmatsobrian.sirohaequ.audio.GainMath
import com.rahmatsobrian.sirohaequ.data.model.EqBand
import kotlin.math.abs
import kotlin.math.ln

private const val MIN_FREQ = 20f
private const val MAX_FREQ = 20000f
private const val MAX_GAIN_DB = 24f

/** Extra invisible touch radius (px) added around each band handle so it's easy to grab with a finger. */
private const val TOUCH_SLOP_PX = 72f

/**
 * Returns a short category label for a frequency, for user-facing context
 * (e.g. "60 Hz — Sub Bass"). Ranges follow common audio-engineering convention.
 */
fun frequencyCategory(freqHz: Float): String = when {
    freqHz < 60f -> "Sub Bass"
    freqHz < 250f -> "Bass"
    freqHz < 500f -> "Low Mid"
    freqHz < 2000f -> "Mid"
    freqHz < 4000f -> "Upper Mid"
    freqHz < 6000f -> "Presence"
    else -> "Treble / Air"
}

private fun formatFreq(freqHz: Float): String =
    if (freqHz >= 1000f) "${(freqHz / 1000f).let { if (it == it.toInt().toFloat()) it.toInt().toString() else "%.1f".format(it) }}kHz"
    else "${freqHz.toInt()}Hz"

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
    modifier: Modifier = Modifier,
    showBandLabels: Boolean = true
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val pointColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(bands) {
                // Track which band the current drag gesture "grabbed" so the
                // finger can wander horizontally without jumping to another
                // band mid-drag — this is what made dragging feel unreliable.
                var activeBandId: Int? = null
                detectDragGestures(
                    onDragStart = { start ->
                        activeBandId = nearestBandWithinTouchSlop(bands, start, size.width.toFloat(), size.height.toFloat())
                    },
                    onDragEnd = { activeBandId = null },
                    onDragCancel = { activeBandId = null },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val bandId = activeBandId ?: return@detectDragGestures
                        val deltaDb = -dragAmount.y / size.height * (MAX_GAIN_DB * 2)
                        val newGain = (currentGain(bands, bandId) + deltaDb).coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB)
                        onBandGainChange(bandId, newGain)
                    }
                )
            }
            .pointerInput(bands) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val nearest = nearestBandWithinTouchSlop(bands, offset, size.width.toFloat(), size.height.toFloat())
                            ?: nearestBand(bands, offset.x, size.width.toFloat())
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

        // Band handles — visual radius bumped up from 10px so they're easy to
        // see and roughly match the actual (larger) touch target below.
        bands.forEach { band ->
            val x = freqToX(band.frequencyHz, w)
            val y = h / 2f - (band.gainDb / MAX_GAIN_DB) * (h / 2f)
            drawCircle(pointColor, radius = 18f, center = Offset(x, y))
            drawCircle(Color.White, radius = 7f, center = Offset(x, y))
        }
    }

    if (showBandLabels && bands.isNotEmpty()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            bands.sortedBy { it.frequencyHz }.forEach { band ->
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(
                        formatFreq(band.frequencyHz),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                    Text(
                        frequencyCategory(band.frequencyHz),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                    Text(
                        "%+.1f dB".format(band.gainDb),
                        style = MaterialTheme.typography.labelSmall,
                        color = pointColor
                    )
                }
            }
        }
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
    return bands.minByOrNull { abs(ln(it.frequencyHz) - ln(freq)) }?.id
}

/**
 * Finds the band whose handle is closest to [touch] in actual pixel space,
 * but only if it's within [TOUCH_SLOP_PX] of the handle center. This is what
 * makes grabbing a point forgiving — you don't have to land exactly on the
 * small dot, a generous circle around it counts too.
 */
private fun nearestBandWithinTouchSlop(
    bands: List<EqBand>,
    touch: Offset,
    width: Float,
    height: Float
): Int? {
    if (bands.isEmpty()) return null
    var bestId: Int? = null
    var bestDist = Float.MAX_VALUE
    bands.forEach { band ->
        val x = freqToX(band.frequencyHz, width)
        val y = height / 2f - (band.gainDb / MAX_GAIN_DB) * (height / 2f)
        val dist = kotlin.math.hypot((x - touch.x).toDouble(), (y - touch.y).toDouble()).toFloat()
        if (dist < bestDist) {
            bestDist = dist
            bestId = band.id
        }
    }
    return if (bestDist <= TOUCH_SLOP_PX) bestId else null
}

private fun currentGain(bands: List<EqBand>, bandId: Int): Float =
    bands.firstOrNull { it.id == bandId }?.gainDb ?: 0f
