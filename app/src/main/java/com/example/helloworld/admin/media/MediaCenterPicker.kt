package com.example.helloworld.admin.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MediaCenterPicker(
    title: String,
    allowedTypes: Set<String>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    val viewModel: MediaCenterViewModel = viewModel()
    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                }
                val candidates = items.filter { it.type in allowedTypes }
                if (!loading && candidates.isEmpty()) {
                    Text("No compatible media has been uploaded yet.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(candidates, key = { it.id }) { item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { onSelected(item.url) },
                                tonalElevation = 2.dp,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(item.title.ifBlank { item.url }, style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(3.dp))
                                    Text(item.type, style = MaterialTheme.typography.labelSmall)
                                    if (item.description.isNotBlank()) {
                                        Spacer(Modifier.height(3.dp))
                                        Text(item.description, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
