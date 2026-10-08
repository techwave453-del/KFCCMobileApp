@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class BibleGameEntry(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val available: Boolean
)

@Composable
fun BibleGamesHubScreen(
    onBack: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenMemoryVerse: () -> Unit,
    onOpenGuessCharacter: () -> Unit,
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    var showComingSoon by remember { mutableStateOf<String?>(null) }

    val games = remember {
        listOf(
            BibleGameEntry(
                "Bible Quiz",
                "Multiple-choice questions with explanations and Bible references.",
                Icons.Default.HelpOutline,
                true
            ),
            BibleGameEntry(
                "Memory Verse",
                "Memorize Scripture through progressive recall challenges.",
                Icons.Default.FormatQuote,
                true
            ),
            BibleGameEntry(
                "Guess the Character",
                "Identify Bible characters from clues, stories and references.",
                Icons.Default.Groups,
                true
            ),
            BibleGameEntry(
                "Fill in the Blank",
                "Complete key Scripture phrases and Bible story statements.",
                Icons.Default.Extension,
                false
            ),
            BibleGameEntry(
                "Daily Challenge",
                "A fresh Bible challenge each day with streak and XP support.",
                Icons.Default.Flag,
                false
            ),
            BibleGameEntry(
                "Choose Your Path",
                "Make story decisions and discover the biblical lesson behind each path.",
                Icons.Default.Psychology,
                false
            ),
            BibleGameEntry(
                "Journey to Jerusalem",
                "Progress through a guided biblical adventure.",
                Icons.Default.Map,
                false
            ),
            BibleGameEntry(
                "Character Missions",
                "Complete mission sequences based on important Bible characters.",
                Icons.Default.AutoStories,
                false
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("Bible Games", fontWeight = FontWeight.Bold)
                    Text(
                        "Learn Scripture through play",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.SportsEsports, contentDescription = "Bible Games")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SportsEsports,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                "Kanisa Bible Games",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "A native game hub designed to grow into the Bible-game experience from CYA Connect Hub, while sharing Kanisa authentication, Supabase data, progress and notifications.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            items(games, key = { it.title }) { game ->
                Card(
                    onClick = {
                        when (game.title) {
                            "Bible Quiz" -> onOpenQuiz()
                            "Memory Verse" -> onOpenMemoryVerse()
                            "Guess the Character" -> onOpenGuessCharacter()
                            else -> showComingSoon = game.title
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(50.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    game.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    game.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!game.available) {
                                    Spacer(Modifier.width(8.dp))
                                    AssistChip(
                                        onClick = { showComingSoon = game.title },
                                        label = { Text("Coming soon") }
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                game.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    showComingSoon?.let { title ->
        AlertDialog(
            onDismissRequest = { showComingSoon = null },
            title = { Text(title) },
            text = {
                Text(
                    "This game is now part of the Kanisa game architecture and is scheduled for native implementation. It will use the same Kanisa account, Supabase backend and progress system rather than opening a separate app."
                )
            },
            confirmButton = {
                TextButton(onClick = { showComingSoon = null }) {
                    Text("OK")
                }
            }
        )
    }
}
