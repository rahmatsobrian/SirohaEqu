@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rahmatsobrian.sirohaequ.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rahmatsobrian.sirohaequ.audio.EngineState
import com.rahmatsobrian.sirohaequ.data.model.DeviceProfile
import com.rahmatsobrian.sirohaequ.data.model.EqBand

@Composable
fun MainScreen(
    state: EqualizerUiState,
    onNavigateDeviceTuning: () -> Unit,
    onNavigatePresets: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateAbout: () -> Unit = {},
    onToggleEq: (Boolean) -> Unit,
    onPreampChange: (Float) -> Unit,
    onApplyDeviceProfile: (DeviceProfile) -> Unit = {},
    onDeleteDeviceProfile: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Siroha Equ",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        if (state.engineState == EngineState.ACTIVE) {
                            Text(
                                "Equalizer aktif",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Pengaturan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DeviceStatusCard(
                    deviceName = state.activeDeviceName,
                    engineState = state.engineState,
                    lastError = state.lastError,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                AudioVisualizerCard(
                    bands = state.activePreset.bands,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                EqToggleCard(
                    presetName = state.activePreset.name,
                    eqEnabled = state.eqEnabled,
                    onToggle = onToggleEq,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                EqGraph(
                    bands = state.activePreset.bands,
                    onBandGainChange = { _, _ -> },
                    onBandReset = {},
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                Text(
                    "Menu",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NavCard(
                        title = "Device Tuning",
                        subtitle = "EQ & Bass",
                        icon = Icons.Filled.Tune,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateDeviceTuning
                    )
                    NavCard(
                        title = "Presets",
                        subtitle = "${state.presets.size} preset",
                        icon = Icons.Filled.GraphicEq,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigatePresets
                    )
                }
            }

            if (state.deviceProfiles.isNotEmpty()) {
                item {
                    Text(
                        "Profil Perangkat",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                items(state.deviceProfiles, key = { it.id }) { profile ->
                    DeviceProfileItem(
                        profile = profile,
                        isCurrentDevice = profile.identity?.productName == state.activeDeviceName,
                        onApply = { onApplyDeviceProfile(profile) },
                        onDelete = { onDeleteDeviceProfile(profile.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                AssistChip(
                    onClick = onNavigateAbout,
                    label = { Text("Tentang Siroha Equ") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Layers,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun DeviceStatusCard(
    deviceName: String?,
    engineState: EngineState,
    lastError: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Perangkat Aktif",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        deviceName ?: "Tidak terdeteksi",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                EngineStatusChip(engineState)
            }
            if (lastError != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    lastError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun AudioVisualizerCard(
    bands: List<EqBand>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Audio Visualizer",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                val w = size.width
                val h = size.height
                val centerY = h / 2f

                // Grid lines
                for (i in 0..4) {
                    val y = h * i / 4f
                    drawLine(
                        outlineColor,
                        Offset(0f, y),
                        Offset(w, y),
                        strokeWidth = 0.5f
                    )
                }

                if (bands.isNotEmpty()) {
                    // Animated wave path
                    val wavePath = Path().apply {
                        var x = 0f
                        while (x <= w) {
                            val normalizedX = x / w
                            val freqIndex = (normalizedX * (bands.size - 1)).toInt().coerceIn(0, bands.size - 1)
                            val band = bands[freqIndex]
                            val amplitude = (band.gainDb / 24f) * (h / 2.5f)
                            val wave = kotlin.math.sin(normalizedX * 6f * Math.PI + phase.toDouble()).toFloat()
                            val y = centerY - amplitude * wave - (h * 0.05f)
                            if (x == 0f) moveTo(x, y) else lineTo(x, y)
                            x += 2f
                        }
                    }
                    drawPath(wavePath, primaryColor, style = Stroke(width = 3f))

                    // Second wave (offset)
                    val wave2Path = Path().apply {
                        var x = 0f
                        while (x <= w) {
                            val normalizedX = x / w
                            val freqIndex = (normalizedX * (bands.size - 1)).toInt().coerceIn(0, bands.size - 1)
                            val band = bands[freqIndex]
                            val amplitude = (band.gainDb / 24f) * (h / 3f)
                            val wave = kotlin.math.sin(normalizedX * 4f * Math.PI + phase.toDouble() + 1.0).toFloat()
                            val y = centerY - amplitude * wave + (h * 0.05f)
                            if (x == 0f) moveTo(x, y) else lineTo(x, y)
                            x += 2f
                        }
                    }
                    drawPath(wave2Path, tertiaryColor.copy(alpha = 0.5f), style = Stroke(width = 2f))
                } else {
                    // Flat line when no bands
                    drawLine(
                        primaryColor.copy(alpha = 0.3f),
                        Offset(0f, centerY),
                        Offset(w, centerY),
                        strokeWidth = 2f
                    )
                }
            }
        }
    }
}

@Composable
private fun EqToggleCard(
    presetName: String,
    eqEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (eqEnabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Equalizer",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Preset: $presetName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = eqEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
private fun EngineStatusChip(engineState: EngineState) {
    val (label, color) = when (engineState) {
        EngineState.ACTIVE -> "Aktif" to MaterialTheme.colorScheme.primary
        EngineState.DEGRADED -> "Terbatas" to MaterialTheme.colorScheme.tertiary
        EngineState.FAILED -> "Gagal" to MaterialTheme.colorScheme.error
        EngineState.IDLE -> "Idle" to MaterialTheme.colorScheme.outline
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f)
        )
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun NavCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DeviceProfileItem(
    profile: DeviceProfile,
    isCurrentDevice: Boolean,
    onApply: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentDevice)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${profile.category.name} \u2022 ${profile.presetId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isCurrentDevice) {
                    Text(
                        "Perangkat saat ini",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Row {
                FilledTonalIconButton(
                    onClick = onApply,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        Icons.Filled.Layers,
                        contentDescription = "Terapkan",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Hapus",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
