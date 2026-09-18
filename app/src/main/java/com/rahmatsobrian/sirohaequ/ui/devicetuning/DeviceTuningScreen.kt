@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rahmatsobrian.sirohaequ.ui.devicetuning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rahmatsobrian.sirohaequ.data.model.DeviceCategory
import com.rahmatsobrian.sirohaequ.data.model.DeviceProfile
import com.rahmatsobrian.sirohaequ.data.model.ProcessingChain
import com.rahmatsobrian.sirohaequ.ui.EqGraph
import com.rahmatsobrian.sirohaequ.ui.EqualizerUiState
import com.rahmatsobrian.sirohaequ.ui.frequencyCategory

@Composable
fun DeviceTuningScreen(
    state: EqualizerUiState,
    onBandChange: (bandId: Int, gainDb: Float) -> Unit,
    onBandQChange: (bandId: Int, q: Float) -> Unit = { _, _ -> },
    onSaveProfile: (DeviceProfile) -> Unit,
    onBassBoostChange: (Float) -> Unit = {},
    onSubBassChange: (Float) -> Unit = {},
    onTrebleBoostChange: (Float) -> Unit = {},
    onAirBoostChange: (Float) -> Unit = {},
    onPreampChange: (Float) -> Unit = {},
    onBalanceChange: (Float) -> Unit = {},
    onBack: () -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var showBassSection by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Device Tuning",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DeviceInfoCard(state)
            }

            item {
                PreampSlider(
                    value = state.preampDb,
                    onValueChange = onPreampChange
                )
            }

            item {
                Text(
                    "Parametric EQ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Geser titik pada grafik atau pakai slider di bawah. Ketuk dua kali untuk reset ke 0 dB.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                EqGraph(
                    bands = state.activePreset.bands,
                    onBandGainChange = onBandChange,
                    onBandReset = { bandId -> onBandChange(bandId, 0f) },
                    showBandLabels = false,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(state.activePreset.bands) { band ->
                BandSlider(
                    frequencyHz = band.frequencyHz,
                    gainDb = band.gainDb,
                    qFactor = band.qFactor,
                    onGainChange = { onBandChange(band.id, it) },
                    onQChange = { onBandQChange(band.id, it) }
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                BassTrebleSection(
                    chain = state.activePreset.chain,
                    onBassBoostChange = onBassBoostChange,
                    onSubBassChange = onSubBassChange,
                    onTrebleBoostChange = onTrebleBoostChange,
                    onAirBoostChange = onAirBoostChange
                )
            }

            item {
                BalanceSection(
                    balance = state.activePreset.chain.balance,
                    monoEnabled = state.activePreset.chain.monoEnabled,
                    stereoWidth = state.activePreset.chain.stereoWidthPercent,
                    onBalanceChange = onBalanceChange
                )
            }

            item {
                FilledTonalButton(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Simpan sebagai Profil Perangkat")
                }
            }
        }
    }

    if (showSaveDialog) {
        SaveProfileDialog(
            deviceName = state.activeDeviceName,
            activePresetId = state.activePreset.id,
            onConfirm = { profile ->
                onSaveProfile(profile)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false }
        )
    }
}

@Composable
private fun DeviceInfoCard(state: EqualizerUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Informasi Perangkat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                state.activeDeviceName ?: "Tidak terdeteksi",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(8.dp))
            val caps = state.capabilities
            if (caps != null) {
                CapabilityRow(
                    label = "Equalizer",
                    available = caps.equalizerAvailable,
                    detail = if (caps.equalizerAvailable) "${caps.equalizerBandCount} band native" else null
                )
                CapabilityRow(
                    label = "Bass Boost",
                    available = caps.bassBoostAvailable,
                    detail = if (caps.bassBoostAvailable) {
                        if (caps.bassBoostStrengthSupported) "Strength supported" else "Basic"
                    } else null
                )
                CapabilityRow(
                    label = "Loudness Enhancer",
                    available = caps.loudnessEnhancerAvailable
                )
                CapabilityRow(
                    label = "Preset Reverb",
                    available = caps.presetReverbAvailable
                )
                if (caps.unavailableReasons.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    caps.unavailableReasons.forEach { (feature, reason) ->
                        Text(
                            "\u2022 $feature: $reason",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Text(
                    "Hubungkan sesi audio dari aplikasi pemutar musik terlebih dahulu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CapabilityRow(label: String, available: Boolean, detail: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(
            if (available) {
                if (detail != null) "Tersedia \u2022 $detail" else "Tersedia"
            } else "Tidak tersedia",
            style = MaterialTheme.typography.bodySmall,
            color = if (available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun PreampSlider(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Preamp",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "%+.1f dB".format(value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = -12f..12f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun BandSlider(
    frequencyHz: Float,
    gainDb: Float,
    qFactor: Float,
    onGainChange: (Float) -> Unit,
    onQChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "${formatFreq(frequencyHz)} \u2022 ${frequencyCategory(frequencyHz)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    "%+.1f dB".format(gainDb),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Slider(
                value = gainDb,
                onValueChange = onGainChange,
                valueRange = -24f..24f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Q: %.1f".format(qFactor),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Gain: %+.1f dB".format(gainDb),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BassTrebleSection(
    chain: ProcessingChain,
    onBassBoostChange: (Float) -> Unit,
    onSubBassChange: (Float) -> Unit,
    onTrebleBoostChange: (Float) -> Unit,
    onAirBoostChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Bass & Treble",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            // Bass Boost
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bass Boost", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "%.0f dB".format(chain.bassBoostDb),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = chain.bassBoostDb,
                onValueChange = onBassBoostChange,
                valueRange = 0f..24f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Sub Bass
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sub Bass", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "%.0f dB".format(chain.subBassBoostDb),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = chain.subBassBoostDb,
                onValueChange = onSubBassChange,
                valueRange = 0f..24f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Treble Boost
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Treble Boost", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "%.0f dB".format(chain.trebleBoostDb),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = chain.trebleBoostDb,
                onValueChange = onTrebleBoostChange,
                valueRange = 0f..24f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Air Boost
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Air Boost", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "%.0f dB".format(chain.airBoostDb),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = chain.airBoostDb,
                onValueChange = onAirBoostChange,
                valueRange = 0f..24f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BalanceSection(
    balance: Float,
    monoEnabled: Boolean,
    stereoWidth: Int,
    onBalanceChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Balance & Stereo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            // Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Balance L/R", style = MaterialTheme.typography.bodyMedium)
                Text(
                    when {
                        balance < -0.05f -> "L %.0f%%".format(kotlin.math.abs(balance) * 100)
                        balance > 0.05f -> "R %.0f%%".format(balance * 100)
                        else -> "Center"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = balance,
                onValueChange = onBalanceChange,
                valueRange = -1f..1f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Mono indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Mono: ${if (monoEnabled) "Aktif" else "Nonaktif"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Stereo Width: $stereoWidth%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SaveProfileDialog(
    deviceName: String?,
    activePresetId: String,
    onConfirm: (DeviceProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var displayName by remember { mutableStateOf(deviceName ?: "") }
    var selectedCategory by remember { mutableStateOf(DeviceCategory.OTHER) }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simpan Profil Perangkat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nama Profil") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DeviceCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (displayName.isNotBlank()) {
                        onConfirm(
                            DeviceProfile(
                                id = "profile_${System.currentTimeMillis()}",
                                displayName = displayName,
                                category = selectedCategory,
                                presetId = activePresetId,
                                createdAtEpochMs = System.currentTimeMillis()
                            )
                        )
                    }
                },
                enabled = displayName.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

private fun formatFreq(hz: Float): String =
    if (hz >= 1000f) "%.1f kHz".format(hz / 1000f) else "${hz.toInt()} Hz"
