package com.example.helloworld.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.admin.AdminShell
import com.example.helloworld.admin.AdminViewModel
import com.example.helloworld.data.ChatAuthRepository
import com.example.helloworld.data.ChatMessage
import com.example.helloworld.data.ChatRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    innerPadding: PaddingValues,
    repository: ChatAuthRepository = remember { ChatAuthRepository() },
    chatRepository: ChatRepository = remember { ChatRepository() }
) {
    var signedIn by remember { mutableStateOf(repository.isSignedIn()) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var createAccount by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var verificationPending by remember { mutableStateOf(false) }
    var loginAttempt by remember { mutableStateOf(false) }
    var memberLoginStarted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as Application
    val adminViewModel: AdminViewModel = viewModel(factory = AdminViewModel.Factory(application))
    val adminUser by adminViewModel.user.collectAsState()
    val adminLoading by adminViewModel.isLoading.collectAsState()
    val adminError by adminViewModel.error.collectAsState()

    // A valid administrator session takes the user directly into the same
    // mobile administration shell. There is no second admin login screen.
    if (adminUser != null) {
        AdminShell(adminViewModel, Modifier.fillMaxSize().padding(innerPadding))
        return
    }

    LaunchedEffect(loginAttempt, adminLoading, adminError) {
        if (loginAttempt && !adminLoading && adminError != null && !memberLoginStarted) {
            memberLoginStarted = true
            busy = true
            val result = repository.signInWithUsername(username, password)
            if (result.success) {
                val profile = repository.completeProfile(username)
                if (profile.success) {
                    signedIn = true
                    message = null
                } else {
                    message = profile.message
                }
            } else {
                message = result.message
            }
            busy = false
            loginAttempt = false
            memberLoginStarted = false
        }
    }

    if (signedIn) {
        CommunityChat(username, repository, chatRepository) {
            scope.launch {
                repository.signOut()
                signedIn = false
                username = ""
                password = ""
                message = null
            }
        }
        return
    }

    Surface(Modifier.fillMaxSize().padding(innerPadding)) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Chat, contentDescription = null)
            Text("KFCC Chat", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                if (verificationPending) "We've sent a verification email. Verify your email, then tap the button below."
                else if (createAccount) "Create one simple account to join the conversation."
                else "Welcome back. Sign in to continue chatting or access administration.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(Modifier.height(24.dp))

            if (verificationPending) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Check your email", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("After verifying your email, return here and sign in with your username and password.")
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = { verificationPending = false; createAccount = false; message = "Enter your username and password to continue." },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("I've verified my email") }
                    }
                }
            } else {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        OutlinedTextField(
                            username,
                            { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }; message = null },
                            Modifier.fillMaxWidth(),
                            label = { Text("Username") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            singleLine = true,
                            prefix = { Text("@") }
                        )

                        if (createAccount) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                email,
                                { email = it },
                                Modifier.fillMaxWidth(),
                                label = { Text("Email") },
                                leadingIcon = { Icon(Icons.Default.MailOutline, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            password,
                            { password = it; message = null },
                            Modifier.fillMaxWidth(),
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = {
                                busy = true
                                message = null
                                if (createAccount) {
                                    scope.launch {
                                        val result = repository.signUp(email, password, username)
                                        busy = false
                                        message = result.message
                                        if (result.success) verificationPending = true
                                    }
                                } else {
                                    // First check the existing secure website admin
                                    // endpoint. If the credentials are not an admin
                                    // account, the username-based chat auth function
                                    // performs normal Supabase password verification.
                                    memberLoginStarted = false
                                    loginAttempt = true
                                    adminViewModel.clearError()
                                    adminViewModel.login(username, password)
                                }
                            },
                            enabled = !busy && !adminLoading && username.isNotBlank() && password.isNotBlank() && (!createAccount || email.isNotBlank()),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            if (busy || (loginAttempt && adminLoading)) CircularProgressIndicator(strokeWidth = 2.dp) else Text(if (createAccount) "Create Account" else "Sign In")
                        }
                        message?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 14.dp))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (createAccount) "Already have an account?" else "New to KFCC Chat?")
                    TextButton(onClick = { createAccount = !createAccount; message = null }) { Text(if (createAccount) "Sign in" else "Create account") }
                }
            }
        }
    }
}

@Composable
private fun CommunityChat(username: String, repository: ChatAuthRepository, chatRepository: ChatRepository, onSignOut: () -> Unit) {
    var roomId by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        chatRepository.joinCommunity()
            .onSuccess { roomId = it }
            .onFailure { error = it.message ?: "Unable to open community chat." }
        loading = false
    }

    LaunchedEffect(roomId) {
        val id = roomId ?: return@LaunchedEffect
        while (true) {
            chatRepository.getMessages(id)
                .onSuccess {
                    messages = it
                    error = null
                }
                .onFailure { error = it.message }
            delay(3000)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Chat, null)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("KFCC Community", fontWeight = FontWeight.Bold)
                    Text(if (username.isBlank()) "Community chat" else "@$username", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onSignOut) { Text("Sign out") }
            }
            HorizontalDivider()
            if (loading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (messages.isEmpty()) item { Text("Welcome to the KFCC community. Start the conversation.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(messages, key = { it.id }) { ChatBubble(it, it.senderId == repository.currentUserId()) }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall) }
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(input, { if (it.length <= 1000) input = it }, Modifier.weight(1f), placeholder = { Text("Write a message…") }, maxLines = 4)
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val id = roomId ?: return@IconButton
                        sending = true
                        scope.launch {
                            chatRepository.sendMessage(id, input)
                                .onSuccess { input = "" }
                                .onFailure { error = it.message }
                            sending = false
                        }
                    },
                    enabled = !sending && input.trim().isNotEmpty() && roomId != null
                ) {
                    if (sending) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Send, "Send")
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, own: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (own) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (own) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                Text(if (own) "You" else "Community member", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(message.message, Modifier.padding(top = 2.dp))
            }
        }
    }
}
