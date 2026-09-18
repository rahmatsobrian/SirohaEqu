@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rahmatsobrian.sirohaequ.ui.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Diagnostics
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rahmatsobrian.sirohaequ.data.ThemeMode
import com.rahmatsobrian.sirohaequ.ui.EqualizerUiState

@Composable
fun SettingsScreen(
    state: EqualizerUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenAbout: () -> Unit = {},
    onPreampChange: (Float) -> Unit = {},
    onLimiterChange: (Boolean) -> Unit = {},
    onCrossfeedChange: (Int) -> Unit = {},
    onAutoProfileChange: (Boolean) -> Unit = {},
    onPerformanceModeChange: (String) -> Unit = {},
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pengaturan",
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
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Theme Section
            item {
                SectionHeader(
                    icon = Icons.Filled.ColorLens,
                    title = "Tampilan"
                )
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Filled.Layers,
                    title = "Dynamic Color",
                    subtitle = "Gunakan warna dari wallpaper (Android 12+)",
                    checked = state.dynamicColor,
                    onCheckedChange = onDynamicColorChange
                )
            }

            item {
                Text(
                    "Mode Tema",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> "Sistem"
                                        ThemeMode.LIGHT -> "Terang"
                                        ThemeMode.DARK -> "Gelap"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> Icons.Filled.PhoneAndroid
                                        ThemeMode.LIGHT -> Icons.Filled.LightMode
                                        ThemeMode.DARK -> Icons.Filled.DarkMode
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            // Audio Processing Section
            item {
                SectionHeader(
                    icon = Icons.Filled.Tune,
                    title = "Audio Processing"
                )
            }

            item {
                SettingsSliderItem(
                    icon = Icons.Filled.Adjust,
                    title = "Preamp",
                    value = state.preampDb,
                    valueRange = -12f..12f,
                    valueLabel = "%+.1f dB".format(state.preampDb),
                    onValueChange = onPreampChange
                )
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Filled.Bolt,
                    title = "Limiter",
                    subtitle = "Mencegah clipping/peaking",
                    checked = state.limiterEnabled,
                    onCheckedChange = onLimiterChange
                )
            }

            item {
                SettingsSliderItem(
                    icon = Icons.Filled.Headphones,
                    title = "Crossfeed",
                    value = state.crossfeedPercent.toFloat(),
                    valueRange = 0f..100f,
                    valueLabel = "${state.crossfeedPercent}%",
                    onValueChange = { onCrossfeedChange(it.toInt()) }
                )
            }

            // Device Section
            item {
                SectionHeader(
                    icon = Icons.Filled.PhoneAndroid,
                    title = "Perangkat"
                )
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Filled.Headphones,
                    title = "Auto Profile",
                    subtitle = "Otomatis terapkan profil saat perangkat terdeteksi",
                    checked = state.autoProfile,
                    onCheckedChange = onAutoProfileChange
                )
            }

            // Performance Section
            item {
                SectionHeader(
                    icon = Icons.Filled.Speed,
                    title = "Performa"
                )
            }

            item {
                Text(
                    "Mode Performa",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("battery_saver" to "Hemat Baterai", "balanced" to "Seimbang", "performance" to "Performa").forEach { (mode, label) ->
                        FilterChip(
                            selected = state.performanceMode == mode,
                            onClick = { onPerformanceModeChange(mode) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    }
                }
            }

            // Diagnostics Section
            item {
                SectionHeader(
                    icon = Icons.Filled.Diagnostics,
                    title = "Diagnostik"
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Logs & Diagnostic Report") },
                    supportingContent = { Text("Lihat, ekspor, atau bagikan log lokal") },
                    leadingContent = {
                        Icon(Icons.Filled.AudioFile, contentDescription = null, modifier = Modifier.size(24.dp))
                    },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                SettingsNavItem(
                    title = "Buka Diagnostics",
                    subtitle = "Lihat log dan ekspor laporan",
                    icon = Icons.Filled.Diagnostics,
                    onClick = onOpenDiagnostics
                )
            }

            // About Section
            item {
                SectionHeader(
                    icon = Icons.Filled.Info,
                    title = "Lainnya"
                )
            }

            item {
                SettingsNavItem(
                    title = "Tentang Siroha Equ",
                    subtitle = "Versi 1.0.0",
                    icon = Icons.Filled.Info,
                    onClick = onOpenAbout
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 20.dp, 16.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    )
}

@Composable
private fun SettingsSliderItem(
    icon: ImageVector,
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    valueLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
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
private fun SettingsNavItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            leadingContent = {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            },
            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) }
        )
    }
}
