package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.data.BibleGameQuestion
import com.example.helloworld.data.BibleGameRepository
import com.example.helloworld.ui.viewmodel.BibleGameProgressViewModel

@Composable
fun GuessCharacterScreen(
    onBack: () -> Unit,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    progressViewModel: BibleGameProgressViewModel = viewModel()
) {
    var questions by remember { mutableStateOf<List<BibleGameQuestion>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var index by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var score by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    var recorded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        questions = BibleGameRepository().loadGameQuestions("guess_character", 10)
        loading = false
        finished = questions.isEmpty()
    }

    LaunchedEffect(finished, recorded) {
        if (finished && questions.isNotEmpty() && !recorded) {
            recorded = true
            progressViewModel.recordQuizResult(score, questions.size)
        }
    }

    Column(Modifier.fillMaxSize().padding(innerPadding)) {
        TopAppBar(
            title = { Text("Guess the Character", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Groups, contentDescription = "Back to Bible Games")
                }
            }
        )

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            questions.isEmpty() -> EmptyCharacterGame()
            finished -> CharacterResult(score, questions.size, onBack)
            else -> {
                val question = questions[index]
                Column(
                    Modifier.fillMaxSize().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { (index + 1).toFloat() / questions.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Character ${index + 1} of ${questions.size}", style = MaterialTheme.typography.labelLarge)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Groups, null, Modifier.size(46.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            Text("Who am I?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text(question.question, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        }
                    }

                    question.options.forEachIndexed { answerIndex, option ->
                        val isCorrect = question.correctAnswerIndex == answerIndex
                        val container = when {
                            selected >= 0 && isCorrect -> MaterialTheme.colorScheme.primaryContainer
                            selected == answerIndex && !isCorrect -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        Card(
                            onClick = { if (selected < 0) selected = answerIndex },
                            colors = CardDefaults.cardColors(containerColor = container),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${('A'.code + answerIndex).toChar()}", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(14.dp))
                                Text(option, Modifier.weight(1f))
                                if (selected >= 0 && isCorrect) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (selected >= 0) {
                        Text(
                            if (selected == question.correctAnswerIndex) "Correct! ${question.explanation}" else "Not quite. ${question.explanation}",
                            color = if (selected == question.correctAnswerIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text(question.reference, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = {
                                if (selected == question.correctAnswerIndex) score++
                                if (index == questions.lastIndex) finished = true
                                else { index++; selected = -1 }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (index == questions.lastIndex) "Finish" else "Next Character")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCharacterGame() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Groups, null, Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("No character challenges are available yet.", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("The built-in challenges will be available after the next app update.", textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CharacterResult(score: Int, total: Int, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.EmojiEvents, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(18.dp))
        Text("Round complete!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("${score} / ${total} characters identified", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Your progress has been saved.", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onBack, Modifier.fillMaxWidth()) { Text("Back to Bible Games") }
    }
}