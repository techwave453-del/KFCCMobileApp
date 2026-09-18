package com.example.helloworld.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.admin.AdminRepositoryProvider
import com.example.helloworld.admin.AdminViewModel
import com.example.helloworld.auth.UnifiedAuthRepository
import com.example.helloworld.auth.UnifiedAuthResult
import com.example.helloworld.data.ChatAuthRepository
import com.example.helloworld.data.ChatMessage
import com.example.helloworld.ui.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    innerPadding: PaddingValues,
    viewModel: ChatViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel(
        factory = AdminViewModel.Factory(LocalContext.current.applicationContext as Application)
    ),
    onAdminLoginSuccess: () -> Unit = {}
) {
    val signedIn by viewModel.signedIn.collectAsState()
    val context = LocalContext.current
    val authRepository = remember(context) {
        UnifiedAuthRepository(
            AdminRepositoryProvider.get(context.applicationContext),
            ChatAuthRepository()
        )
    }

    var username by remember { mutableStateOf("") }
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var createAccount by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var verificationPending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (signedIn) {
        CommunityChat(
            username = username,
            viewModel = viewModel,
            onSignOut = {
                viewModel.signOut()
                username = ""
                message = null
            }
        )
        return
    }

    Surface(Modifier.fillMaxSize().padding(innerPadding)) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Chat,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))

            Text(
                "KFCC Chat",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                when {
                    verificationPending ->
                        "We've sent a verification email. Verify your email, then tap the button below."
                    createAccount ->
                        "Create one simple account to join the conversation."
                    else ->
                        "Welcome back. Sign in to continue chatting."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(24.dp))

            if (verificationPending) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "Check your email",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "After verifying your email, return here and sign in with the same account."
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = {
                                verificationPending = false
                                createAccount = false
                                message = "Enter your verified account details to continue."
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("I've verified my email")
                        }
                    }
                }
            } else {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        if (createAccount) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = {
                                    username = it.lowercase()
                                        .filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Username") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                singleLine = true,
                                prefix = { Text("@") }
                            )
                            Spacer(Modifier.height(12.dp))
                        }

                        OutlinedTextField(
                            value = identifier,
                            onValueChange = { identifier = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    if (createAccount) "Email"
                                    else "Username"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (createAccount) Icons.Default.MailOutline
                                    else Icons.Default.Person,
                                    null
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (createAccount)
                                    KeyboardType.Email
                                else
                                    KeyboardType.Text
                            ),
                            singleLine = true
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
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
                                scope.launch {
                                    if (createAccount) {
                                        val result = ChatAuthRepository()
                                            .signUp(identifier, password, username)

                                        if (result.success) {
                                            verificationPending = true
                                        } else {
                                            message = result.message
                                                ?: "Unable to create your account."
                                        }
                                    } else {
                                        try {
                                            when (
                                                val result = authRepository.signIn(
                                                    identifier,
                                                    password
                                                )
                                            ) {
                                                is UnifiedAuthResult.Administrator -> {
                                                    username = result.username
                                                    viewModel.onSignedIn()
                                                    onAdminLoginSuccess()
                                                }

                                                is UnifiedAuthResult.Member -> {
                                                    username = result.username
                                                    viewModel.onSignedIn()
                                                }
                                            }
                                        } catch (error: Exception) {
                                            message = error.message
                                                ?: "Incorrect username or password."
                                        }
                                    }
                                    busy = false
                                }
                            },
                            enabled = !busy &&
                                identifier.isNotBlank() &&
                                password.isNotBlank() &&
                                (!createAccount || username.isNotBlank()),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (busy) {
                                CircularProgressIndicator(strokeWidth = 2.dp)
                            } else {
                                Text(if (createAccount) "Create Account" else "Sign In")
                            }
                        }

                        message?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 14.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (createAccount)
                            "Already have an account?"
                        else
                            "New to KFCC Chat?"
                    )
                    TextButton(
                        onClick = {
                            createAccount = !createAccount
                            message = null
                        }
                    ) {
                        Text(if (createAccount) "Sign in" else "Create account")
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityChat(
    username: String,
    viewModel: ChatViewModel,
    onSignOut: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val sending by viewModel.sending.collectAsState()
    val error by viewModel.error.collectAsState()
    val roomId by viewModel.roomId.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Chat, null)
                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text("KFCC Community", fontWeight = FontWeight.Bold)
                    Text(
                        if (username.isBlank()) "Community chat" else "@$username",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                TextButton(onClick = onSignOut) {
                    Text("Sign out")
                }
            }

            HorizontalDivider()

            if (loading && messages.isEmpty()) {
                Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Text(
                                "Welcome to the KFCC community. Start the conversation.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(messages, key = { it.id }) {
                        ChatBubble(
                            it,
                            it.senderId == viewModel.currentUserId()
                        )
                    }
                }
            }

            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            if (it.length <= 1000) input = it
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Write a message…") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            viewModel.sendMessage(input)
                            input = ""
                        },
                        enabled = !sending &&
                            input.trim().isNotEmpty() &&
                            roomId != null
                    ) {
                        if (sending) {
                            CircularProgressIndicator(
                                Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Send, "Send")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, own: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (own) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (own)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                Text(
                    if (own) "You" else "Community member",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    message.message,
                    Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
