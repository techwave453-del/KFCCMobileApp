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
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.data.ChatAuthRepository
import com.example.helloworld.data.ChatMessage
import com.example.helloworld.ui.ChatViewModel
import com.example.helloworld.admin.AdminViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    innerPadding: PaddingValues,
    viewModel: ChatViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel(factory = AdminViewModel.Factory(LocalContext.current.applicationContext as Application)),
    onAdminLoginSuccess: () -> Unit = {}
) {
    val signedIn by viewModel.signedIn.collectAsState()
    val authRepository = remember { ChatAuthRepository() }
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var createAccount by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var verificationPending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (signedIn) {
        CommunityChat(viewModel)
        return
    }

    Surface(Modifier.fillMaxSize().padding(innerPadding)) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("KFCC Community Chat", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                text = when {
                    verificationPending -> "We've sent a verification email. Please verify your email, then tap the button below to sign in."
                    createAccount -> "Create an account to join the conversation and share with the church community."
                    else -> "Welcome back. Sign in with your email or admin username to continue."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            if (verificationPending) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Check your email", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Verify your email by clicking the link we sent, then return here.")
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { verificationPending = false; createAccount = false; message = "Enter your verified account details to continue." },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("I've verified my email") }
                    }
                }
            } else {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        if (createAccount) {
                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { usernameInput = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' } },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Choose Username") },
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
                            label = { Text(if (createAccount) "Email Address" else "Email or Admin Username") },
                            leadingIcon = { Icon(if (createAccount) Icons.Default.MailOutline else Icons.Default.Person, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = if (createAccount) KeyboardType.Email else KeyboardType.Text),
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)

                        if (message != null) {
                            Text(message!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                        }

                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                busy = true
                                message = null
                                scope.launch {
                                    try {
                                        if (createAccount) {
                                            val result = authRepository.signUp(identifier.trim(), password, usernameInput.trim())
                                            if (result.success) {
                                                verificationPending = true
                                            } else {
                                                message = result.message
                                            }
                                        } else {
                                            val loginId = identifier.trim()

                                            if (loginId.contains("@")) {
                                                // Email identifiers belong to the Supabase community account flow.
                                                val result = authRepository.signIn(loginId, password)
                                                if (result.success) {
                                                    val profile = authRepository.completeProfile()
                                                    if (profile.success) {
                                                        viewModel.onSignedIn()
                                                    } else {
                                                        message = profile.message
                                                    }
                                                } else {
                                                    message = result.message
                                                }
                                            } else {
                                                // Non-email identifiers belong to the website admin account flow.
                                                // Do not race Supabase auth and do not send a username to email auth.
                                                val authenticated = adminViewModel.authenticate(loginId, password)
                                                if (authenticated) {
                                                    onAdminLoginSuccess()
                                                } else {
                                                    message = adminViewModel.error.value ?: "Invalid administrator username or password."
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        message = e.message ?: "Unable to sign in. Please try again."
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                            enabled = !busy && identifier.isNotBlank() && password.isNotBlank() && (!createAccount || usernameInput.isNotBlank()),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (busy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            else Text(if (createAccount) "Create Account" else "Sign In")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (createAccount) "Already have an account?" else "New to KFCC Community?")
                    TextButton(onClick = { createAccount = !createAccount; message = null }) {
                        Text(if (createAccount) "Sign in" else "Create account")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityChat(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val sending by viewModel.sending.collectAsState()
    val error by viewModel.error.collectAsState()
    val roomId by viewModel.roomId.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text("KFCC Community", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Online conversation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::initChat) { Icon(Icons.Default.Refresh, "Refresh") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
            HorizontalDivider()

            if (loading && messages.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (messages.isEmpty() && !loading) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No messages yet. Start the conversation!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    items(messages, key = { it.id }) { message ->
                        ChatBubble(message, message.senderId == viewModel.currentUserId())
                    }
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = viewModel::clearError, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Dismiss") }
            }

            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.Bottom
                ) {
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
                        onClick = {
                            viewModel.sendMessage(input)
                            input = ""
                        },
                        enabled = !sending && input.trim().isNotEmpty() && roomId != null,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        if (sending) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Send, "Send", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, own: Boolean) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val date = remember(message.createdAt) {
        try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            format.parse(message.createdAt.substringBefore("."))
        } catch (e: Exception) {
            Date()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (own) Arrangement.End else Arrangement.Start
    ) {
        if (!own) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(32.dp).align(Alignment.Bottom),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.width(8.dp))
        }

        Surface(
            color = if (own) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (own) 16.dp else 4.dp,
                bottomEnd = if (own) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!own) {
                    Text(
                        text = message.senderProfile?.username?.let { "@$it" } ?: "Community member",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (date != null) timeFormat.format(date) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
