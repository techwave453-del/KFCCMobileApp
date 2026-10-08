package com.example.helloworld.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.BibleGameCategory
import com.example.helloworld.data.BibleGameQuestion
import com.example.helloworld.data.BibleGameRepository
import kotlinx.coroutines.delay

private enum class GameMode(val label: String, val description: String) {
    QUIZ("Bible Quiz", "Take your time and learn from every answer."),
    TIMED("Timed Challenge", "Answer as many questions as you can in 60 seconds.")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleGamesScreen(onBack: () -> Unit = {}) {
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
            started = questions.isNotEmpty()
        }
    }

    LaunchedEffect(started, mode, answered, questionIndex) {
        if (!started || mode != GameMode.TIMED || answered != null) return@LaunchedEffect
        while (secondsLeft > 0 && answered == null && started) {
            delay(1000)
            secondsLeft--
        }
        if (secondsLeft == 0 && answered == null) started = false
    }

    val gameFinished =
        started && questions.isNotEmpty() &&
            questionIndex >= questions.lastIndex && answered != null
    val timedOut =
        !started && questions.isNotEmpty() && secondsLeft == 0 && answered == null

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
            modifier = Modifier.weight(1f),
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
                "timeout" -> GameResult(score, questions.size, true, ::startGame)
                "result" -> GameResult(score, questions.size, false, ::startGame)
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
                            if (answered == null) {
                                answered = selected
                                if (selected == question.correctAnswerIndex) score++
                            }
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
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val horizontalPadding = if (this@BoxWithConstraints.maxWidth < 360.dp) 12.dp else 16.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontalPadding, 8.dp, horizontalPadding, 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            )
                            .padding(if (this@BoxWithConstraints.maxWidth < 360.dp) 18.dp else 22.dp)
                    ) {
                        Icon(
                            Icons.Default.SportsEsports,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Grow through play",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Test your Bible knowledge, learn from the answers, and build a stronger understanding of Scripture.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Text("Game mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GameMode.values().forEach { option ->
                        FilterChip(
                            selected = mode == option,
                            onClick = { onModeChange(option) },
                            label = { Text(option.label) },
                            leadingIcon = {
                                Icon(
                                    if (option == GameMode.TIMED) Icons.Default.Timer
                                    else Icons.Default.MenuBook,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 13.dp),
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
                        Spacer(Modifier.width(12.dp))
                        Text(
                            option.label,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold
                        )
                        if (category == option) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Game")
                }
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

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 360.dp) 12.dp else 16.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontalPadding, 8.dp, horizontalPadding, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Question $questionNumber of $total",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { questionNumber.toFloat() / total.coerceAtLeast(1).toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            if (timed) "$secondsLeft s" else "Score $score",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(if (this@BoxWithConstraints.maxWidth < 360.dp) 16.dp else 18.dp)
                    ) {
                        Text(
                            question.question,
                            style = if (this@BoxWithConstraints.maxWidth < 360.dp) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.titleLarge
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(7.dp))
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
                val containerColor = when {
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
                    colors = CardDefaults.cardColors(containerColor = containerColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(34.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    ('A'.code + index).toChar().toString(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            question.options[index],
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (selectedAnswer != null && isCorrect) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Correct",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else if (isSelected) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = "Incorrect",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (selectedAnswer != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (answeredCorrectly) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Column(Modifier.padding(if (this@BoxWithConstraints.maxWidth < 360.dp) 14.dp else 16.dp)) {
                            Text(
                                if (answeredCorrectly) "Correct!" else "Not quite",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(question.explanation, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(7.dp))
                            Text(
                                question.reference,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            if (questionNumber == total) "Finish Game" else "Next Question",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
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

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val horizontalPadding = if (this@BoxWithConstraints.maxWidth < 360.dp) 16.dp else 24.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontalPadding, 20.dp, horizontalPadding, 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Icon(
                    if (timedOut) Icons.Default.TimerOff else Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(68.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (timedOut) "Time's up!" else "Game complete",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    "$score / $total",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    message,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            item {
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Play Again")
                }
            }
        }
    }
}

@Composable
private fun LoadingGame() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Preparing your Bible game…", textAlign = TextAlign.Center)
        }
    }
}
