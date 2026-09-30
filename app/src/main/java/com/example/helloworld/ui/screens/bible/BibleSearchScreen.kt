package com.example.helloworld.ui.screens.bible

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.bible.BibleSearchResult
import com.example.helloworld.data.bible.KfccBibleRepository
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleSearchScreen(
    onBack: () -> Unit,
    onOpenChapter: (String, Int) -> Unit
) {
    val repository = remember { KfccBibleRepository() }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<BibleSearchResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        delay(250)
        searching = true
        results = runCatching { repository.search("kjv", q) }.getOrDefault(emptyList())
        searching = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Scripture", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search words or phrases") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searching) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            )

            Spacer(Modifier.size(14.dp))

            when {
                query.trim().length < 2 -> Text(
                    "Search the Bible by word or phrase.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                !searching && results.isEmpty() -> Text(
                    "No Scripture matches found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results, key = { "${it.bookId}-${it.chapter}-${it.verse}" }) { result ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onOpenChapter(result.bookId, result.chapter)
                            },
                            tonalElevation = 1.dp
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "${result.bookName} ${result.chapter}:${result.verse}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    result.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                                Row(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Open chapter",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("›", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
