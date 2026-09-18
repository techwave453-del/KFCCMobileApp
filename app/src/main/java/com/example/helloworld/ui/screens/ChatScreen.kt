package com.example.helloworld.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.admin.AdminViewModel
import com.example.helloworld.data.ChatMessage
import com.example.helloworld.ui.ChatViewModel
import com.example.helloworld.ui.ChurchViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    innerPadding: PaddingValues,
    viewModel: ChatViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel(factory = AdminViewModel.Factory(LocalContext.current.applicationContext as Application)),
    onAdminLoginSuccess: () -> Unit = {}
) {
    val churchViewModel: ChurchViewModel = viewModel()
    val churchInfo by churchViewModel.churchInfo.collectAsState()
    val signedIn by viewModel.signedIn.collectAsState()

    if (signedIn) {
        CommunityChat(viewModel, churchInfo.churchName)
    } else {
        UnifiedAuthScreen(
            churchInfo = churchInfo,
            chatViewModel = viewModel,
            adminViewModel = adminViewModel,
            innerPadding = innerPadding,
            onMemberSignedIn = viewModel::initChat,
            onAdminLoginSuccess = onAdminLoginSuccess
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityChat(viewModel: ChatViewModel, churchName: String) {
    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val sending by viewModel.sending.collectAsState()
    val error by viewModel.error.collectAsState()
    val roomId by viewModel.roomId.collectAsState()
    var input by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text(churchName.ifBlank { "Community Chat" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Community messages", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = { IconButton(onClick = viewModel::initChat) { Icon(Icons.Default.Refresh, "Refresh") } }
            )
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search messages") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp)
            )
            HorizontalDivider()

            if (loading && messages.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                val visibleMessages = messages.filter { search.isBlank() || it.message.contains(search, ignoreCase = true) }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (visibleMessages.isEmpty()) {
                        item { Box(Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) { Text("No messages yet. Start the conversation!", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    }
                    items(visibleMessages, key = { it.id }) { message -> ChatBubble(message, message.senderId == viewModel.currentUserId()) }
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = viewModel::clearError, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Dismiss") }
            }

            Surface(tonalElevation = 2.dp, shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(10.dp).navigationBarsPadding().imePadding(), verticalAlignment = Alignment.Bottom) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { if (it.length <= 1000) input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Write a message…") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.sendMessage(input); input = "" },
                        enabled = !sending && input.trim().isNotEmpty() && roomId != null
                    ) { if (sending) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Send, "Send", tint = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, own: Boolean) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val date = remember(message.createdAt) {
        try { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(message.createdAt.substringBefore('.')) }
        catch (_: Exception) { null }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (own) Arrangement.End else Arrangement.Start) {
        if (!own) { Icon(Icons.Default.AccountCircle, null, Modifier.size(32.dp).align(Alignment.Bottom), tint = MaterialTheme.colorScheme.outline); Spacer(Modifier.width(8.dp)) }
        Surface(
            color = if (own) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!own) Text(message.senderProfile?.username?.let { "@$it" } ?: "Community member", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(message.message)
                Text(if (date != null) timeFormat.format(date) else "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .65f), modifier = Modifier.align(Alignment.End))
            }
        }
    }
}
