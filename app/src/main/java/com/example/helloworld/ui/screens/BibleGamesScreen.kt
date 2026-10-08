package com.example.helloworld.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.data.BibleGameCategory
import com.example.helloworld.data.BibleGameQuestion
import com.example.helloworld.data.BibleGameRepository
import kotlinx.coroutines.delay

private enum class GameMode(val label: String, val description: String) {
    QUIZ("Bible Quiz", "Take your time and learn from every answer."),
    TIMED("Timed Challenge", "Answer as many questions as you can in 60 seconds.")
}

@Composable
fun BibleGamesScreen(
    onBack: () -> Unit = {}
) {
    var mode by rememberSaveable { mutableStateOf(GameMode.QUIZ) }
    var category by rememberSaveable { mutableStateOf(BibleGameCategory.ALL) }
    var questions by remember { mutableStateOf<List<BibleGameQuestion>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var started by rememberSaveable { mutableStateOf(false) }
    var questionIndex by rememberSaveable { mutableIntStateOf(0) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var answered by rememberSaveable { mutableStateOf<Int?>(null) }
    var secondsLeft by rememberSaveable { mutableIntStateOf(60) }

    val repository = remember { BibleGameRepository() }

    fun startGame() {
        loading = true
        started = false
        questionIndex = 0
        score = 0
        answered = null
    }

    LaunchedEffect(loading) {
        if (loading) {
            questions = repository.loadQuestions(category, 10)
            secondsLeft = 60
            loading = false
            started = true
        }
    }

    LaunchedEffect(started, mode, answered, questionIndex) {
        if (!started || mode != GameMode.TIMED || answered != null) return@LaunchedEffect
        while (secondsLeft > 0 && answered == null && started) {
            delay(1000)
            secondsLeft--
        }
        if (secondsLeft == 0 && answered == null) {
            started = false
        }
    }

    val gameFinished = started && questions.isNotEmpty() && questionIndex >= questions.lastIndex && answered != null
    val timedOut = !started && questions.isNotEmpty() && secondsLeft == 0 && answered == null

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Bible Games") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        AnimatedContent(
            targetState = when {
                loading -> "loading"
                timedOut -> "timeout"
                gameFinished || (!started && questions.isNotEmpty() && answered == null) -> "result"
                started && questions.isNotEmpty() -> "question"
                else -> "home"
            },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "Bible games content"
        ) { screen ->
            when (screen) {
                "loading" -> LoadingGame()
                "timeout" -> GameResult(
                    score = score,
                    total = questions.size,
                    timedOut = true,
                    onPlayAgain = ::startGame
                )
                "result" -> GameResult(
                    score = score,
                    total = questions.size,
                    timedOut = false,
                    onPlayAgain = ::startGame
                )
                "question" -> {
                    val question = questions[questionIndex]
                    GameQuestionCard(
                        question = question,
                        questionNumber = questionIndex + 1,
                        total = questions.size,
                        score = score,
                        secondsLeft = secondsLeft,
                        timed = mode == GameMode.TIMED,
                        selectedAnswer = answered,
                        onAnswer = { selected ->
                            if (answered != null) return@GameQuestionCard
                            answered = selected
                            if (selected == question.correctAnswerIndex) score++
                        },
                        onNext = {
                            if (questionIndex < questions.lastIndex) {
                                questionIndex++
                                answered = null
                            } else {
                                started = false
                                answered = null
                            }
                        }
                    )
                }
                else -> GameHome(
                    mode = mode,
                    category = category,
                    onModeChange = { mode = it },
                    onCategoryChange = { category = it },
                    onStart = ::startGame
                )
            }
        }
    }
}

@Composable
private fun GameHome(
    mode: GameMode,
    category: BibleGameCategory,
    onModeChange: (GameMode) -> Unit,
    onCategoryChange: (BibleGameCategory) -> Unit,
    onStart: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.SportsEsports,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("Grow through play", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Test your Bible knowledge, learn from the answers, and build a stronger understanding of Scripture.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        item {
            Text("Game mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameMode.values().forEach { option ->
                    FilterChip(
                        selected = mode == option,
                        onClick = { onModeChange(option) },
                        label = { Text(option.label) },
                        leadingIcon = {
                            Icon(
                                if (option == GameMode.TIMED) Icons.Default.Timer else Icons.Default.MenuBook,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                mode.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }

        items(BibleGameCategory.values().toList(), key = { it.name }) { option ->
            Card(
                onClick = { onCategoryChange(option) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (category == option) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when (option) {
                            BibleGameCategory.PEOPLE -> Icons.Default.Groups
                            BibleGameCategory.PLACES -> Icons.Default.Place
                            BibleGameCategory.FAITH_AND_LIFE -> Icons.Default.Favorite
                            BibleGameCategory.NEW_TESTAMENT -> Icons.Default.AutoStories
                            BibleGameCategory.OLD_TESTAMENT -> Icons.Default.Book
                            BibleGameCategory.ALL -> Icons.Default.Shuffle
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(option.label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    if (category == option) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start Game")
            }
        }
    }
}

@Composable
private fun GameQuestionCard(
    question: BibleGameQuestion,
    questionNumber: Int,
    total: Int,
    score: Int,
    secondsLeft: Int,
    timed: Boolean,
    selectedAnswer: Int?,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit
) {
    val answeredCorrectly = selectedAnswer == question.correctAnswerIndex

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Question $questionNumber of $total", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { questionNumber.toFloat() / total.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        if (timed) "$secondsLeft s" else "Score $score",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        question.question,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        question.category.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        items(question.options.indices.toList()) { index ->
            val isSelected = selectedAnswer == index
            val isCorrect = index == question.correctAnswerIndex
            val color = when {
                selectedAnswer == null -> MaterialTheme.colorScheme.surfaceVariant
                isCorrect -> MaterialTheme.colorScheme.primaryContainer
                isSelected -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }

            Card(
                onClick = { onAnswer(index) },
                enabled = selectedAnswer == null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = color)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(('A'.code + index).toChar().toString(), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(question.options[index], modifier = Modifier.weight(1f))
                    if (selectedAnswer != null && isCorrect) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = MaterialTheme.colorScheme.primary)
                    } else if (isSelected) {
                        Icon(Icons.Default.Cancel, contentDescription = "Incorrect", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (selectedAnswer != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (answeredCorrectly) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            if (answeredCorrectly) "Correct!" else "Not quite",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(question.explanation)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            question.reference,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (questionNumber == total) "Finish Game" else "Next Question")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun GameResult(
    score: Int,
    total: Int,
    timedOut: Boolean,
    onPlayAgain: () -> Unit
) {
    val percentage = if (total == 0) 0 else (score * 100) / total
    val message = when {
        percentage >= 90 -> "Excellent Bible knowledge!"
        percentage >= 70 -> "Great work! Keep growing."
        percentage >= 50 -> "Good effort. Keep learning."
        else -> "Keep playing and discover more."
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (timedOut) Icons.Default.TimerOff else Icons.Default.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(18.dp))
        Text(
            if (timedOut) "Time's up!" else "Game complete",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Text("$score / $total", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Default.Replay, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Play Again")
        }
    }
}

@Composable
private fun LoadingGame() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Preparing your Bible game…")
        }
    }
}
