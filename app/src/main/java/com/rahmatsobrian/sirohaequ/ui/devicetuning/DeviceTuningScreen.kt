@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rahmatsobrian.sirohaequ.ui.devicetuning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rahmatsobrian.sirohaequ.data.model.DeviceProfile
import com.rahmatsobrian.sirohaequ.ui.EqGraph
import com.rahmatsobrian.sirohaequ.ui.EqualizerUiState

@Composable
fun DeviceTuningScreen(
    state: EqualizerUiState,
    onBandChange: (bandId: Int, gainDb: Float) -> Unit,
    onSaveProfile: (DeviceProfile) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Tuning") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Informasi Perangkat", style = MaterialTheme.typography.titleMedium)
                        Text(
                            state.activeDeviceName ?: "Tidak terdeteksi",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val caps = state.capabilities
                        if (caps != null) {
                            Text(
                                "Equalizer native: ${if (caps.equalizerAvailable) "${caps.equalizerBandCount} band" else "tidak tersedia"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Bass Boost: ${if (caps.bassBoostAvailable) "tersedia" else "tidak tersedia"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            caps.unavailableReasons.forEach { (feature, reason) ->
                                Text(
                                    "• $feature: $reason",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Text(
                                "Kemampuan audio belum terdeteksi — hubungkan sesi audio dari aplikasi pemutar musik terlebih dahulu.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                Text("Parametric EQ", style = MaterialTheme.typography.titleMedium)
                EqGraph(
                    bands = state.activePreset.bands,
                    onBandGainChange = onBandChange,
                    onBandReset = { bandId -> onBandChange(bandId, 0f) }
                )
            }

            items(state.activePreset.bands) { band ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${formatFreq(band.frequencyHz)} — ${"%.1f".format(band.gainDb)} dB")
                        Slider(
                            value = band.gainDb,
                            onValueChange = { onBandChange(band.id, it) },
                            valueRange = -24f..24f
                        )
                    }
                }
            }
        }
    }
}

private fun formatFreq(hz: Float): String =
    if (hz >= 1000f) "%.1f kHz".format(hz / 1000f) else "${hz.toInt()} Hz"
