@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.bible.KfccBibleRepository
import com.example.helloworld.data.BibleGamePlayerStats
import com.example.helloworld.data.BibleGameRepository
import com.example.helloworld.ui.viewmodel.BibleGameProgressViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

private data class MemoryVerseTarget(
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    val reference: String
)

@Composable
fun MemoryVerseScreen(
    onBack: () -> Unit,
    onOpenReference: (String) -> Unit = {},
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    val repository = remember { KfccBibleRepository() }
    val gameRepository = remember { BibleGameRepository() }
    val scope = rememberCoroutineScope()
    val targets = remember {
        listOf(
            MemoryVerseTarget("PSA", 23, 1, "Psalm 23:1"),
            MemoryVerseTarget("PRO", 3, 5, "Proverbs 3:5"),
            MemoryVerseTarget("JOS", 1, 9, "Joshua 1:9"),
            MemoryVerseTarget("ISA", 41, 10, "Isaiah 41:10"),
            MemoryVerseTarget("JHN", 3, 16, "John 3:16"),
            MemoryVerseTarget("ROM", 8, 28, "Romans 8:28"),
            MemoryVerseTarget("PHP", 4, 13, "Philippians 4:13")
        )
    }

    var targetIndex by remember { mutableIntStateOf(0) }
    var verseText by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var revealed by remember { mutableStateOf(false) }
    var completed by remember { mutableIntStateOf(0) }
    val progressViewModel: BibleGameProgressViewModel = viewModel()
    val stats = progressViewModel.stats
    var completionRecorded by remember { mutableStateOf(false) }

    fun loadTarget(index: Int) {
        val target = targets[index]
        loading = true
        revealed = false
        verseText = null
        scope.launch {
            val chapter = runCatching {
                repository.getChapter("kjv", target.bookId, target.chapter)
            }.getOrNull()
            verseText = chapter?.verses?.firstOrNull { it.number == target.verse }?.text
            loading = false
        }
    }

    LaunchedEffect(targetIndex) {
        completionRecorded = false
        loadTarget(targetIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("Memory Verse", fontWeight = FontWeight.Bold)
                    Text(
                        "Hide it. Remember it. Live it.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.FormatQuote, contentDescription = "Back")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "Memory Challenge",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Read the verse carefully, hide it, then try to recall the key words before revealing it again.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                stats?.let { progress ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Your progress", fontWeight = FontWeight.Bold)
                                Text(progress.xp.toString() + " XP • " + progress.currentStreak + " day streak", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(completed.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                TextButton(
                    onClick = { onOpenReference(targets[targetIndex].reference) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        targets[targetIndex].reference,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.FormatQuote,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))

                        when {
                            loading -> CircularProgressIndicator()
                            revealed && verseText != null -> Text(
                                "“$verseText”",
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                            verseText != null -> Text(
                                "The verse is hidden.\nRecall as much as you can.",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            else -> Text(
                                "This verse could not be loaded. Try another challenge.",
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        if (!loading && verseText != null) {
                            OutlinedButton(onClick = { revealed = !revealed }) {
                                Icon(Icons.Default.Visibility, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (revealed) "Hide Verse" else "Reveal Verse")
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        if (!completionRecorded) {
                            completionRecorded = true
                            completed++
                            progressViewModel.recordMemoryVerseCompleted()
                        }
                        targetIndex = (targetIndex + 1) % targets.size
                    },
                    enabled = !loading && verseText != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("I remembered it — Next")
                }
            }

            item {
                OutlinedButton(
                    onClick = { loadTarget(targetIndex) },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Try Again")
                }
            }

            item {
                Text(
                    "Verses completed: $completed",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
