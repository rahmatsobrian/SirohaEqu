@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rahmatsobrian.sirohaequ.ui.presets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rahmatsobrian.sirohaequ.data.model.Preset

@Composable
fun PresetScreen(
    presets: List<Preset>,
    activePresetId: String,
    onSelect: (Preset) -> Unit,
    onSaveAs: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Presets") },
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                TextButton(onClick = { onSaveAs("Preset Baru ${System.currentTimeMillis() % 1000}") }) {
                    Text("+ Simpan pengaturan saat ini sebagai preset baru")
                }
            }

            items(presets, key = { it.id }) { preset ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSelect(preset) },
                    colors = if (preset.id == activePresetId) {
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    } else {
                        CardDefaults.cardColors()
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(preset.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (preset.isBuiltIn) "Built-in" else "Kustom",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (!preset.isBuiltIn) {
                            IconButton(onClick = { onDelete(preset.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Hapus")
                            }
                        }
                    }
                }
            }
        }
    }
}
