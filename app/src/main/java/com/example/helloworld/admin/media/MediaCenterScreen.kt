package com.example.helloworld.admin.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MediaCenterScreen(
    modifier: Modifier = Modifier,
    viewModel: MediaCenterViewModel = viewModel()
) {
    val items by viewModel.items.collectAsStateCompat()
    val loading by viewModel.loading.collectAsStateCompat()
    val error by viewModel.error.collectAsStateCompat()

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Media Center", style = MaterialTheme.typography.headlineSmall)
                    Text("Church images, videos, audio and external media", style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = viewModel::refresh, enabled = !loading) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh media")
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            if (loading && items.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Loading media library…")
                }
                return@Column
            }

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Text("Pull refresh to try again.", style = MaterialTheme.typography.bodySmall)
            }

            if (!loading && items.isEmpty() && error == null) {
                Text("No media has been published yet.", style = MaterialTheme.typography.bodyLarge)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (item.isVideo) Icons.Default.VideoLibrary else Icons.Default.Image,
                                contentDescription = null
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 13.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (item.featured && item.isVideo) {
                                        Icon(Icons.Default.Star, contentDescription = "Featured video", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Text(
                                    "${item.type.ifBlank { "media" }} · ${item.category}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                if (item.description.isNotBlank()) {
                                    Text(item.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Text(
                                    if (item.published) "Published" else "Unpublished",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.published) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateCompat(): androidx.compose.runtime.State<T> =
    androidx.compose.runtime.collectAsState(this)
