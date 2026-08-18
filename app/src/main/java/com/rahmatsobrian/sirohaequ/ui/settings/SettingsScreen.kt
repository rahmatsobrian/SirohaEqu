package com.rahmatsobrian.sirohaequ.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rahmatsobrian.sirohaequ.data.ThemeMode
import com.rahmatsobrian.sirohaequ.ui.EqualizerUiState

@Composable
fun SettingsScreen(
    state: EqualizerUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onOpenDiagnostics: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
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
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Text(
                    "Tampilan",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Dynamic Color") },
                    supportingContent = { Text("Gunakan warna dari wallpaper (Android 12+)") },
                    trailingContent = {
                        Switch(checked = state.dynamicColor, onCheckedChange = onDynamicColorChange)
                    }
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.values().forEach { mode ->
                        androidx.compose.material3.FilterChip(
                            selected = state.themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            label = { Text(mode.name) }
                        )
                    }
                }
            }

            item {
                Text(
                    "Diagnostik",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(16.dp, 20.dp, 16.dp, 4.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Logs & Diagnostic Report") },
                    supportingContent = { Text("Lihat, ekspor, atau bagikan log lokal") },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                androidx.compose.material3.TextButton(
                    onClick = onOpenDiagnostics,
                    modifier = Modifier.padding(16.dp, 0.dp)
                ) {
                    Text("Buka Diagnostics →")
                }
            }
        }
    }
}
