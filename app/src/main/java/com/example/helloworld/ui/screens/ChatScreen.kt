package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.ChatAuthRepository
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    repository: ChatAuthRepository = remember { ChatAuthRepository() },
) {
    var signedIn by remember { mutableStateOf(repository.isSignedIn()) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var createAccount by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (signedIn) {
        ChatWelcome(
            username = username,
            onSignOut = {
                scope.launch {
                    repository.signOut()
                    signedIn = false
                    message = null
                }
            },
        )
        return
    }

    Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.padding(bottom = 12.dp))
            Text("KFCC Chat", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                if (createAccount) "Create one simple account to join the conversation." else "Welcome back. Sign in to continue chatting.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    if (createAccount) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Username") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            prefix = { Text("@") },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            busy = true
                            message = null
                            scope.launch {
                                val result = if (createAccount) {
                                    repository.signUp(email, password, username)
                                } else {
                                    repository.signIn(email, password)
                                }
                                busy = false
                                message = result.message
                                if (result.success && !result.needsEmailVerification) {
                                    val profile = repository.completeProfile()
                                    if (profile.success) {
                                        signedIn = true
                                        message = null
                                    } else {
                                        message = profile.message
                                    }
                                }
                            }
                        },
                        enabled = !busy && email.isNotBlank() && password.isNotBlank() && (!createAccount || username.isNotBlank()),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        if (busy) CircularProgressIndicator(strokeWidth = 2.dp) else Text(if (createAccount) "Create Account" else "Sign In")
                    }

                    message?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (createAccount) "Already have an account?" else "New to KFCC Chat?")
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = {
                    createAccount = !createAccount
                    message = null
                }) {
                    Text(if (createAccount) "Sign in" else "Create account")
                }
            }
        }
    }
}

@Composable
private fun ChatWelcome(username: String, onSignOut: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.padding(bottom = 12.dp))
            Text("You're signed in", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                if (username.isBlank()) "KFCC Chat is ready for your profile." else "Welcome, @$username",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                "Community chat is being connected next. Your account is already using the shared KFCC Supabase identity.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(20.dp))
            TextButton(onClick = onSignOut) { Text("Sign out") }
        }
    }
}
