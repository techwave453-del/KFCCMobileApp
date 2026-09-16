package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.ChurchInfo
import com.example.helloworld.events.Event

@Composable
fun SearchScreen(
    info: ChurchInfo,
    events: List<Event>,
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    var query by rememberSaveable { mutableStateOf("") }
    val term = query.trim()
    val results = buildList {
        info.services.forEach { add("Service" to it.title) }
        info.links.forEach { add("Resource" to it.title) }
        events.forEach { add("Event" to it.title) }
    }.filter { term.isBlank() || it.second.contains(term, ignoreCase = true) }

    Column(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search sermons, events, services...") },
            label = { Text("Search") }
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            if (results.isEmpty()) {
                item { Text("No results found.", modifier = Modifier.padding(16.dp)) }
            } else {
                items(results) { result ->
                    ListItem(
                        headlineContent = { Text(result.second) },
                        supportingContent = { Text(result.first) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
