package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.ChatAuthRepository
import com.example.helloworld.data.ChatProfile
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(innerPadding: PaddingValues) {
    val repository = remember { ChatAuthRepository() }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ChatProfile?>(null) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(repository.currentEmail().orEmpty()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var signedIn by remember { mutableStateOf(repository.isSignedIn()) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        loading = true
        error = null
        repository.getProfile()
            .onSuccess {
                profile = it
                username = it?.username.orEmpty()
            }
            .onFailure { error = it.message ?: "Unable to load your profile." }
        email = repository.currentEmail().orEmpty()
        signedIn = repository.isSignedIn()
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("My Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        if (loading) {
            CircularProgressIndicator()
            return@Column
        }

        if (!signedIn) {
            Text("Sign in through Chat to manage your account profile.")
            return@Column
        }

        Text(email, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it.lowercase().removePrefix("@") },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Username") },
            singleLine = true,
            supportingText = { Text("3–20 characters: letters, numbers, _ or .") },
        )

        Spacer(Modifier.height(8.dp))
        Text(
            "Your username is the public identity used in KFCC Chat.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    saving = true
                    message = null
                    error = null
                    val result = repository.completeProfile(username)
                    if (result.success) {
                        message = "Profile saved."
                        repository.getProfile().onSuccess { profile = it }
                    } else {
                        error = result.message ?: "Unable to save your profile."
                    }
                    saving = false
                }
            },
            enabled = !saving && username.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (saving) CircularProgressIndicator(strokeWidth = 2.dp) else Text("Save profile")
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                scope.launch {
                    repository.signOut()
                    signedIn = false
                    profile = null
                    message = "You have been signed out."
                }
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Sign out") }

        message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
