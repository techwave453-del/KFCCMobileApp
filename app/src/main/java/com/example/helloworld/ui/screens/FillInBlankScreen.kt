package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.data.BibleGameRepository
import com.example.helloworld.data.BibleGameQuestion
import com.example.helloworld.ui.viewmodel.BibleGameProgressViewModel

@Composable
fun FillInBlankScreen(onBack: () -> Unit, onOpenReference: (String) -> Unit = {}, innerPadding: PaddingValues = PaddingValues(0.dp), progressViewModel: BibleGameProgressViewModel = viewModel()) {
    var questions by remember { mutableStateOf(emptyList<BibleGameQuestion>()) }
    var loading by remember { mutableStateOf(true) }
    var index by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var score by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var recorded by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(retry) { loading = true; questions = BibleGameRepository().loadGameQuestions("fill_blank", 10); index = 0; selected = -1; score = 0; done = questions.isEmpty(); recorded = false; loading = false }
    LaunchedEffect(done, recorded) { if (done && questions.isNotEmpty() && !recorded) { recorded = true; progressViewModel.recordQuizResult(score, questions.size) } }
    Column(Modifier.fillMaxSize().padding(innerPadding)) {
        TopAppBar(title = { Text("Fill in the Blank") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.Extension, "Back to Bible Games") } })
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            questions.isEmpty() -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text("No published Fill in the Blank questions yet."); Button(onClick = { retry++ }) { Text("Try again") } }
            done -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text("Challenge complete!", style = MaterialTheme.typography.headlineMedium); Text("$score / ${questions.size} correct"); Button(onClick = { retry++ }) { Text("Play again") }; OutlinedButton(onClick = onBack) { Text("Back to Bible Games") } }
            else -> {
                val q = questions[index]
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LinearProgressIndicator(progress = { (index + 1).toFloat() / questions.size }, modifier = Modifier.fillMaxWidth())
                    Text("Question ${index + 1} of ${questions.size}   •   Score $score")
                    Text(q.question, style = MaterialTheme.typography.headlineSmall)
                    q.options.forEachIndexed { i, option ->
                        Card(onClick = { if (selected == -1) selected = i }, enabled = selected == -1, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = when { selected >= 0 && i == q.correctAnswerIndex -> MaterialTheme.colorScheme.primaryContainer; selected == i -> MaterialTheme.colorScheme.errorContainer; else -> MaterialTheme.colorScheme.surfaceVariant })) { Text(option, Modifier.padding(16.dp)) }
                    }
                    if (selected >= 0) {
                        Text(if (selected == q.correctAnswerIndex) "Correct! ${q.explanation}" else "Not quite. ${q.explanation}")
                        TextButton(onClick = { onOpenReference(q.reference) }) { Text(q.reference) }
                        Button(onClick = { if (selected == q.correctAnswerIndex) score++; if (index == questions.lastIndex) done = true else { index++; selected = -1 } }, modifier = Modifier.fillMaxWidth()) { Text(if (index == questions.lastIndex) "Finish" else "Next question") }
                    }
                }
            }
        }
    }
}