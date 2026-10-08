package com.example.helloworld.admin.games

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.admin.BibleGameQuestionAdmin

@Composable
fun AdminBibleGamesScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminBibleGamesViewModel = viewModel(
        factory = AdminBibleGamesViewModel.Factory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as Application
        )
    )
) {
    val questions by viewModel.questions.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val message by viewModel.message.collectAsState()
    val error by viewModel.error.collectAsState()
    var editing by remember { mutableStateOf<BibleGameQuestionAdmin?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Bible Games", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Create, edit and publish questions used by members.", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = viewModel::refresh) { Icon(Icons.Default.Refresh, "Refresh") }
            FilledTonalButton(onClick = { editing = null; showEditor = true }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Add")
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp)) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
        if (loading && questions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(questions, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${item.gameType.replace('_', ' ')} • ${item.category.replace('_', ' ')}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { editing = item; showEditor = true }) { Icon(Icons.Default.Edit, "Edit") }
                                IconButton(onClick = { viewModel.delete(item) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                            }
                            item.options.forEachIndexed { answerIndex, option ->
                                Text(("${('A'.code + answerIndex).toChar()}. $option") + if (answerIndex == item.correctAnswerIndex) " ✓" else "", style = MaterialTheme.typography.bodySmall)
                            }
                            if (item.reference.isNotBlank()) Text(item.reference, style = MaterialTheme.typography.labelSmall)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (item.isPublished) "Published" else "Draft", Modifier.weight(1f), color = if (item.isPublished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Switch(checked = item.isPublished, onCheckedChange = { viewModel.togglePublished(item) })
                            }
                        }
                    }
                }
            }
        }
    }
    if (showEditor) {
        BibleGameQuestionEditor(
            initial = editing ?: BibleGameQuestionAdmin(),
            saving = saving,
            onDismiss = { showEditor = false },
            onSave = { viewModel.save(it); showEditor = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BibleGameQuestionEditor(
    initial: BibleGameQuestionAdmin,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (BibleGameQuestionAdmin) -> Unit
) {
    var question by remember(initial.id) { mutableStateOf(initial.question) }
    var gameType by remember(initial.id) { mutableStateOf(initial.gameType) }
    var category by remember(initial.id) { mutableStateOf(initial.category) }
    var options by remember(initial.id) { mutableStateOf(initial.options + List((4 - initial.options.size).coerceAtLeast(0)) { "" }) }
    var correct by remember(initial.id) { mutableIntStateOf(initial.correctAnswerIndex.coerceIn(0, 3)) }
    var explanation by remember(initial.id) { mutableStateOf(initial.explanation) }
    var reference by remember(initial.id) { mutableStateOf(initial.reference) }
    var published by remember(initial.id) { mutableStateOf(initial.isPublished) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "New Bible Question" else "Edit Bible Question") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                item { OutlinedTextField(question, { question = it }, label = { Text("Question") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                item { GameTypeMenu(gameType) { gameType = it } }
                item { CategoryMenu(category) { category = it } }
                items(4) { answerIndex ->
                    OutlinedTextField(
                        value = options[answerIndex],
                        onValueChange = { value -> options = options.toMutableList().also { it[answerIndex] = value } },
                        label = { Text("Option ${('A'.code + answerIndex).toChar()}") },
                        trailingIcon = { if (correct == answerIndex) Icon(Icons.Default.CheckCircle, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Correct answer", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        options.indices.forEach { answerIndex ->
                            FilterChip(selected = correct == answerIndex, onClick = { correct = answerIndex }, label = { Text(('A'.code + answerIndex).toChar().toString()) })
                        }
                    }
                }
                item { OutlinedTextField(explanation, { explanation = it }, label = { Text("Explanation") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(reference, { reference = it }, label = { Text("Bible reference") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Publish immediately", Modifier.weight(1f)); Switch(published, { published = it }) } }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(initial.copy(gameType = gameType, category = category, question = question, options = options, correctAnswerIndex = correct, explanation = explanation, reference = reference, isPublished = published))
            }, enabled = !saving && question.isNotBlank()) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameTypeMenu(selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.replace('_', ' '),
            onValueChange = {},
            readOnly = true,
            label = { Text("Game type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("quiz","guess_character","memory_verse","fill_blank","daily_challenge","choose_path","journey_jerusalem","character_missions").forEach { value ->
                DropdownMenuItem(text = { Text(value.replace('_', ' ')) }, onClick = { onSelected(value); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryMenu(selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.replace('_', ' '),
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("OLD_TESTAMENT","NEW_TESTAMENT","PEOPLE","PLACES","FAITH_AND_LIFE").forEach { value ->
                DropdownMenuItem(text = { Text(value.replace('_', ' ')) }, onClick = { onSelected(value); expanded = false })
            }
        }
    }
}
