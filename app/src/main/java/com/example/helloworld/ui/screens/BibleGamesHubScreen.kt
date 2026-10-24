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
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape

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
    onOpenFillInBlank: () -> Unit,
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    var showComingSoon by remember { mutableStateOf<String?>(null) }
    val hasScreenBackground = rememberScreenBackgroundUrl("bible_games") != null

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
                true
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

    Box(Modifier.fillMaxSize()) {
        ScreenBackgroundImage(
            screenKey = "bible_games",
            backgroundType = "screen",
            modifier = Modifier.fillMaxSize(),
            overlayAlpha = 0.24f
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            title = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasScreenBackground) Color.Black.copy(alpha = 0.42f) else Color.Transparent
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("Bible Games", fontWeight = FontWeight.Bold, color = if (hasScreenBackground) Color.White else MaterialTheme.colorScheme.onBackground)
                        Text(
                            "Learn Scripture through play",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hasScreenBackground) Color.White.copy(alpha = 0.86f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ScreenBackgroundImage(
                            screenKey = "bible_games",
                            backgroundType = "hero",
                            fallbackUrl = "https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=1200&q=80",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            overlayAlpha = 0f
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.3f),
                                            Color.Black.copy(alpha = 0.56f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.SportsEsports, null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(Modifier.width(6.dp))
                                    Text("BIBLE GAMING HUB", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.Black.copy(alpha = 0.42f)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        "Kanisa Bible Games",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Learn Scripture through interactive quizzes, memory verses, and character challenges.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                            }
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
                            "Fill in the Blank" -> onOpenFillInBlank()
                            else -> showComingSoon = game.title
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
                    )
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
