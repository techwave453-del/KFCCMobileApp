package com.example.helloworld.ui.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.BorderStroke
import com.example.helloworld.data.KanisaAssistantBibleQuote
import com.example.helloworld.ui.KanisaAssistantUiMessage
import com.example.helloworld.ui.KanisaAssistantViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun KanisaAssistantScreen(
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenBibleReference: (String) -> Unit = {},
    viewModel: KanisaAssistantViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val sending by viewModel.sending.collectAsState()
    val error by viewModel.error.collectAsState()
    var input by rememberSaveable { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<KanisaAssistantUiMessage?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    fun send() {
        val draft = input.trim()
        if (draft.isBlank() || sending) return
        viewModel.ask(draft) { success ->
            if (success) input = ""
        }
    }

    val imeVisible = WindowInsets.isImeVisible

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                bottom = if (imeVisible) {
                    0.dp
                } else {
                    innerPadding.calculateBottomPadding()
                }
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = innerPadding.calculateTopPadding()),
            tonalElevation = 2.dp
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Kanisa Assistant", fontWeight = FontWeight.Bold)
                    Text(
                        "Church & Bible Assistant",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (messages.any { it.role == "user" }) {
                    IconButton(
                        onClick = { showClearDialog = true },
                        enabled = !sending
                    ) {
                        Icon(Icons.Default.DeleteSweep, "Clear assistant chat")
                    }
                }
                if (sending) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Suggestion("Service times") { input = "What are the current church service times?" }
            Suggestion("Events") { input = "What events are coming up?" }
            Suggestion("Bible") { input = "What does the Bible say about prayer?" }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                AssistantBubble(
                    message = message,
                    onOpenBibleReference = onOpenBibleReference,
                    onDelete = { if (message.id != 0L) messageToDelete = message },
                    showFollowUps = !sending && message.role == "assistant" && message.id == messages.lastOrNull()?.id,
                    onFollowUp = { prompt -> viewModel.ask(prompt) }
                )
            }
            if (sending) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Kanisa Assistant is thinking…",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        error?.let { message ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        message,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    TextButton(onClick = viewModel::clearError) { Text("Dismiss") }
                }
            }
        }

        Surface(
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            // The parent owns the navigation-bar inset when the IME is hidden.
            // When the IME is visible, remove that parent inset and let imePadding()
            // place the typing area directly against the keyboard.
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        if (it.length <= 2000) {
                            input = it
                            if (error != null) viewModel.clearError()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about the church or Bible…", fontSize = 14.sp) },
                    maxLines = 5,
                    enabled = !sending,
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    supportingText = {
                        if (input.length > 1800) {
                            Text(input.length.toString() + "/2000", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = { send() },
                    modifier = Modifier.size(48.dp),
                    containerColor = if (input.isNotBlank() && !sending) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (input.isNotBlank() && !sending) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                ) {
                    if (sending) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send message")
                    }
                }
            }
        }
    }

    if (messageToDelete != null) {
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Delete message?") },
            text = { Text("This removes this message from your Kanisa Assistant conversation.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMessage(messageToDelete!!.id)
                        messageToDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { messageToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
            title = { Text("Clear assistant chat?") },
            text = { Text("This removes the current conversation from this device. It does not delete church or Bible data.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearMessages()
                        showClearDialog = false
                    }
                ) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun KanisaAssistantText(
    text: String,
    color: Color
) {
    val normalized = text
        .replace("\\r\\n", "\n")
        .replace("\\n", "\n")
        .replace("\\t", "\t")
        .trim()

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        normalized.split("\n").forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.isBlank() -> Spacer(Modifier.height(2.dp))
                line.startsWith("- ") || line.startsWith("• ") -> {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("•", color = color, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            line.drop(2).trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = color
                        )
                    }
                }
                line.startsWith("**") && line.endsWith("**") && line.length > 4 -> {
                    Text(
                        line.removePrefix("**").removeSuffix("**"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                else -> Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun Suggestion(label: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label, fontSize = 11.sp) })
}

@Composable
private fun AssistantBubble(
    message: KanisaAssistantUiMessage,
    onOpenBibleReference: (String) -> Unit,
    onDelete: () -> Unit = {},
    showFollowUps: Boolean = false,
    onFollowUp: (String) -> Unit = {}
) {
    val own = message.role == "user"
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (own) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onDelete
                ),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (own) 18.dp else 5.dp,
                bottomEnd = if (own) 5.dp else 18.dp
            ),
            color = if (own) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Column(Modifier.padding(13.dp)) {
                if (!own) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            null,
                            Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(5.dp))
                        Text("Kanisa Assistant", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(5.dp))
                }
                KanisaAssistantText(
                    text = message.content,
                    color = if (own) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (message.bibleQuotes.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    groupAssistantScriptureQuotes(message.bibleQuotes)
                        .take(6)
                        .forEach { quote ->
                            KanisaAssistantScriptureCard(
                                quote = quote,
                                onClick = { onOpenBibleReference(quote.reference) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    if (showFollowUps) {
                        KanisaAssistantFollowUps(onFollowUp = onFollowUp)
                    }
                } else if (message.bibleReferences.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Referenced Scripture",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(5.dp))
                    message.bibleReferences.forEach { reference ->
                        AssistChip(
                            onClick = { onOpenBibleReference(reference) },
                            label = { Text(reference) },
                            leadingIcon = { Icon(Icons.Default.MenuBook, null, Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KanisaAssistantFollowUps(
    onFollowUp: (String) -> Unit
) {
    Text(
        "Continue studying",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 5.dp)
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AssistChip(
            onClick = { onFollowUp("Explain the Scripture you just shared in simple terms.") },
            label = { Text("Explain this") }
        )
        AssistChip(
            onClick = { onFollowUp("Show me more relevant Bible Scriptures about this topic. Use the Bible context available to you and give the references and exact supplied text.") },
            label = { Text("More Scriptures") }
        )
        AssistChip(
            onClick = { onFollowUp("Explain your previous answer in Kiswahili. Keep any Scripture quotation faithful to the supplied Bible text and clearly identify the reference.") },
            label = { Text("In Kiswahili") }
        )
        AssistChip(
            onClick = { onFollowUp("Give me a short Christian prayer related to what we just discussed.") },
            label = { Text("Pray about it") }
        )
    }
    Spacer(Modifier.height(4.dp))
}

private fun groupAssistantScriptureQuotes(
    quotes: List<KanisaAssistantBibleQuote>
): List<KanisaAssistantBibleQuote> {
    if (quotes.size < 2) return quotes

    val result = mutableListOf<KanisaAssistantBibleQuote>()
    var current = quotes.first()

    fun parseReference(reference: String): Triple<String, Int, Int>? {
        val match = Regex(
            """^(.+?) (\\d+):(\\d+)(?:[-–](\\d+))?$"""
        ).find(reference.trim()) ?: return null

        val book = match.groupValues[1]
        val chapter = match.groupValues[2].toIntOrNull() ?: return null
        val startVerse = match.groupValues[3].toIntOrNull() ?: return null
        val endVerse = match.groupValues[4].toIntOrNull() ?: startVerse
        return Triple(book, chapter, endVerse)
    }

    fun startVerse(reference: String): Int? =
        parseReference(reference)?.let { Triple(it.first, it.second, it.third) }
            ?.third

    fun canMerge(first: KanisaAssistantBibleQuote, second: KanisaAssistantBibleQuote): Boolean {
        if (first.translation != second.translation) return false

        val firstMatch = Regex(
            """^(.+?) (\\d+):(\\d+)(?:[-–](\\d+))?$"""
        ).find(first.reference.trim()) ?: return false
        val secondMatch = Regex(
            """^(.+?) (\\d+):(\\d+)(?:[-–](\\d+))?$"""
        ).find(second.reference.trim()) ?: return false

        val firstBook = firstMatch.groupValues[1]
        val firstChapter = firstMatch.groupValues[2].toIntOrNull() ?: return false
        val firstEnd = firstMatch.groupValues[4].toIntOrNull()
            ?: firstMatch.groupValues[3].toIntOrNull()
            ?: return false

        val secondBook = secondMatch.groupValues[1]
        val secondChapter = secondMatch.groupValues[2].toIntOrNull() ?: return false
        val secondStart = secondMatch.groupValues[3].toIntOrNull() ?: return false

        return firstBook.equals(secondBook, ignoreCase = true) &&
            firstChapter == secondChapter &&
            secondStart == firstEnd + 1
    }

    fun merge(first: KanisaAssistantBibleQuote, second: KanisaAssistantBibleQuote): KanisaAssistantBibleQuote {
        val firstMatch = Regex(
            """^(.+?) (\\d+):(\\d+)(?:[-–](\\d+))?$"""
        ).find(first.reference.trim())
        val secondMatch = Regex(
            """^(.+?) (\\d+):(\\d+)(?:[-–](\\d+))?$"""
        ).find(second.reference.trim())

        if (firstMatch == null || secondMatch == null) return first

        val book = firstMatch.groupValues[1]
        val chapter = firstMatch.groupValues[2]
        val start = firstMatch.groupValues[3]
        val end = secondMatch.groupValues[4].ifBlank { secondMatch.groupValues[3] }

        return first.copy(
            reference = "$book $chapter:$start-$end",
            text = first.text.trim() + " " + second.text.trim()
        )
    }

    quotes.drop(1).forEach { next ->
        if (canMerge(current, next)) {
            current = merge(current, next)
        } else {
            result += current
            current = next
        }
    }
    result += current
    return result
}

@Composable
private fun KanisaAssistantScriptureCard(
    quote: KanisaAssistantBibleQuote,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text("SCRIPTURE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                Text("›", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(7.dp))
            Text("“${quote.text.trim()}”", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(quote.reference, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text(quote.translation, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
