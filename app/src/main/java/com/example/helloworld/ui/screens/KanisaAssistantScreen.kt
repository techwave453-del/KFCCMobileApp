package com.example.helloworld.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.ui.KanisaAssistantUiMessage
import com.example.helloworld.ui.KanisaAssistantViewModel

@Composable
fun KanisaAssistantScreen(
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: KanisaAssistantViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val sending by viewModel.sending.collectAsState()
    val error by viewModel.error.collectAsState()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = innerPadding.calculateTopPadding()),
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
                    modifier = Modifier.size(40.dp),
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
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Suggestion("Service times") { input = "What are the current church service times?" }
            Suggestion("Events") { input = "What events are coming up?" }
            Suggestion("Bible") { input = "What does the Bible say about prayer?" }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                AssistantBubble(message)
            }
            if (sending) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Kanisa Assistant is thinking…", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Surface(tonalElevation = 6.dp, shadowElevation = 12.dp) {
            Row(
                Modifier.fillMaxWidth().imePadding().padding(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.length <= 2000) input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about the church or Bible…", fontSize = 14.sp) },
                    maxLines = 5,
                    shape = RoundedCornerShape(20.dp),
                    enabled = !sending
                )
                Spacer(Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = { viewModel.ask(input); input = "" },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    if (sending) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send")
                    }
                }
            }
        }
    }

    error?.let {
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("Kanisa Assistant") },
            text = { Text(it) },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) { Text("OK") }
            }
        )
    }
}

@Composable
private fun Suggestion(label: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label, fontSize = 11.sp) })
}

@Composable
private fun AssistantBubble(message: KanisaAssistantUiMessage) {
    val own = message.role == "user"
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (own) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomStart = if (own) 18.dp else 5.dp,
                bottomEnd = if (own) 5.dp else 18.dp
            ),
            color = if (own) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Column(Modifier.padding(13.dp)) {
                if (!own) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(5.dp))
                        Text("Kanisa Assistant", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(5.dp))
                }
                Text(
                    message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (own) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (message.bibleReferences.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    message.bibleReferences.forEach { reference ->
                        AssistChip(
                            onClick = { /* Bible navigation will be wired to the Bible screen next. */ },
                            label = { Text(reference) },
                            leadingIcon = { Icon(Icons.Default.MenuBook, null, Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }
    }
}
