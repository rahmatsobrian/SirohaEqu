@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rahmatsobrian.sirohaequ.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rahmatsobrian.sirohaequ.audio.EngineState

@Composable
fun MainScreen(
    state: EqualizerUiState,
    onNavigateDeviceTuning: () -> Unit,
    onNavigatePresets: () -> Unit,
    onNavigateSettings: () -> Unit,
    onToggleEq: (Boolean) -> Unit,
    onPreampChange: (Float) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Siroha Equ") },
                actions = {
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Pengaturan")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Perangkat Aktif", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                                Text(
                                    state.activeDeviceName ?: "Tidak terdeteksi",
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                                )
                            }
                            EngineStatusChip(state.engineState)
                        }
                        if (state.lastError != null) {
                            Text(
                                state.lastError,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Equalizer", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                            Text(
                                "Preset: ${state.activePreset.name}",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(checked = state.eqEnabled, onCheckedChange = onToggleEq)
                    }
                }
            }

            item {
                EqGraph(
                    bands = state.activePreset.bands,
                    onBandGainChange = { _, _ -> /* live preview only on main screen; edits happen in Device Tuning */ },
                    onBandReset = {}
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NavCard(
                        title = "Device Tuning",
                        icon = Icons.Filled.Tune,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateDeviceTuning
                    )
                    NavCard(
                        title = "Presets",
                        icon = Icons.Filled.GraphicEq,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigatePresets
                    )
                }
            }

            items(state.deviceProfiles) { profile ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(profile.displayName, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                        Text(
                            "${profile.category} • preset: ${profile.presetId}",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineStatusChip(engineState: EngineState) {
    val (label, color) = when (engineState) {
        EngineState.ACTIVE -> "Aktif" to androidx.compose.material3.MaterialTheme.colorScheme.primary
        EngineState.DEGRADED -> "Terbatas" to androidx.compose.material3.MaterialTheme.colorScheme.tertiary
        EngineState.FAILED -> "Gagal" to androidx.compose.material3.MaterialTheme.colorScheme.error
        EngineState.IDLE -> "Idle" to androidx.compose.material3.MaterialTheme.colorScheme.outline
    }
    Text(label, color = color, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
}

@Composable
private fun NavCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title)
            Text(title, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
    }
}
