package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.ui.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    innerPadding: PaddingValues,
    viewModel: ProfileViewModel = viewModel(),
    onSignOut: () -> Unit = {},
) {
    val profile by viewModel.profile.collectAsState()
    val email by viewModel.email.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val error by viewModel.error.collectAsState()
    val saved by viewModel.saved.collectAsState()

    var username by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var avatarUrl by rememberSaveable { mutableStateOf("") }
    var initialized by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(profile) {
        if (profile != null && !initialized) {
            username = profile!!.username
            displayName = profile!!.display_name.orEmpty()
            avatarUrl = profile!!.avatar_url.orEmpty()
            initialized = true
        }
    }

    Scaffold(
        modifier = Modifier.padding(innerPadding),
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, "Refresh profile")
                    }
                }
            )
        }
    ) { padding ->
        when {
            loading && profile == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            else -> Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))

                val initial = (displayName.ifBlank { username }.firstOrNull() ?: '?').uppercaseChar().toString()
                Surface(
                    modifier = Modifier.size(92.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (avatarUrl.isBlank()) {
                            Text(initial, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.AccountCircle, null, modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    displayName.ifBlank { "KFCC member" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    email.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.lowercase().removePrefix("@").take(20) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Username") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.AccountCircle, null) },
                        supportingText = { Text("3–20 characters: letters, numbers, _ or .") }
                    )

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it.take(60) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Display name") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = avatarUrl,
                        onValueChange = { avatarUrl = it.take(500) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Avatar URL (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )

                    if (error != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, null)
                                Spacer(Modifier.width(8.dp))
                                Text(error!!, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }

                    if (saved) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Profile saved", color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Button(
                        onClick = { viewModel.save(username, displayName, avatarUrl) },
                        enabled = !saving && username.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Save profile")
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Logout, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign out")
                    }
                }
            }
        }
    }
}
