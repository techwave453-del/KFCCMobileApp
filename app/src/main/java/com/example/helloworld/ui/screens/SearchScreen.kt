package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.ui.SearchItem
import com.example.helloworld.ui.SearchViewModel

@Composable
fun SearchScreen(
    innerPadding: PaddingValues,
    viewModel: SearchViewModel = viewModel()
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                "Search",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Find sermons, events, services and church media.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What are you looking for?") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (query.isNotBlank()) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            Spacer(Modifier.height(18.dp))

            if (query.isBlank()) {
                Text(
                    "Explore",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                SearchHint(icon = Icons.Default.PlayCircle, title = "Sermons & media", text = "Find messages and videos")
                SearchHint(icon = Icons.Default.Event, title = "Events", text = "Find upcoming church events")
                SearchHint(icon = Icons.Default.Church, title = "Services", text = "Find worship service times")
            } else if (query.length < 2) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Type at least 2 characters to search.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (results.isEmpty() && !isSearching) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("No results found", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Try another word or a shorter search.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    "Results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(results) { item ->
                        when (item) {
                            is SearchItem.Media -> SearchResultCard(
                                title = item.item.title,
                                subtitle = item.item.category,
                                icon = Icons.Default.PlayCircle,
                                type = "Sermon / Media"
                            )
                            is SearchItem.Event -> SearchResultCard(
                                title = item.item.title,
                                subtitle = item.item.start_at.replace("T", " "),
                                icon = Icons.Default.Event,
                                type = "Upcoming Event"
                            )
                            is SearchItem.Service -> SearchResultCard(
                                title = item.item.title,
                                subtitle = item.item.time,
                                icon = Icons.Default.Church,
                                type = "Service"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHint(icon: ImageVector, title: String, text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        ListItem(
            leadingContent = {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text(text) }
        )
    }
}

@Composable
private fun SearchResultCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    type: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            headlineContent = {
                Text(title, fontWeight = FontWeight.SemiBold)
            },
            supportingContent = {
                Text("$type • $subtitle")
            }
        )
    }
}
