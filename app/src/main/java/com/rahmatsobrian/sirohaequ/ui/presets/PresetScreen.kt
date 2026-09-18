@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rahmatsobrian.sirohaequ.ui.presets

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.rahmatsobrian.sirohaequ.data.model.Preset

@Composable
fun PresetScreen(
    presets: List<Preset>,
    activePresetId: String,
    onSelect: (Preset) -> Unit,
    onSaveAs: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: (Preset, String) -> Unit = { _, _ -> },
    onExport: (String) -> Unit = {},
    onImport: () -> Unit = {},
    onBack: () -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf<Preset?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Presets",
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Simpan Baru")
                    }
                    FilledTonalButton(
                        onClick = onImport,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Import")
                    }
                }
            }

            items(presets, key = { it.id }) { preset ->
                val isActive = preset.id == activePresetId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    onClick = { onSelect(preset) },
                    colors = if (isActive) {
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    } else {
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                preset.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                buildString {
                                    append(if (preset.isBuiltIn) "Built-in" else "Kustom")
                                    append(" \u2022 ")
                                    append("${preset.bandCount} band")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isActive) {
                                Text(
                                    "Aktif",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Row {
                            IconButton(
                                onClick = { showDuplicateDialog = preset },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = "Duplikat",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onExport(preset.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.FileDownload,
                                    contentDescription = "Export",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (!preset.isBuiltIn) {
                                IconButton(
                                    onClick = { showDeleteConfirmation = preset.id },
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
            }
        }
    }

    if (showSaveDialog) {
        PresetNameDialog(
            title = "Simpan Preset Baru",
            confirmText = "Simpan",
            onConfirm = { name ->
                onSaveAs(name)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false }
        )
    }

    showDuplicateDialog?.let { preset ->
        PresetNameDialog(
            title = "Duplikat Preset",
            confirmText = "Duplikat",
            defaultName = "${preset.name} (Salinan)",
            onConfirm = { name ->
                onDuplicate(preset, name)
                showDuplicateDialog = null
            },
            onDismiss = { showDuplicateDialog = null }
        )
    }

    showDeleteConfirmation?.let { presetId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Hapus Preset?") },
            text = { Text("Preset yang dihapus tidak dapat dikembalikan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(presetId)
                        showDeleteConfirmation = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun PresetNameDialog(
    title: String,
    confirmText: String,
    defaultName: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(defaultName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Preset") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
