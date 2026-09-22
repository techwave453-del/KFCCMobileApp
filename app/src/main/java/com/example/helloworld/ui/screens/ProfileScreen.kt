package com.example.helloworld.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.helloworld.admin.AdminViewModel
import com.example.helloworld.data.ChatAuthRepository
import com.example.helloworld.data.ChatProfile
import com.example.helloworld.ui.ChatViewModel
import com.example.helloworld.ui.ChurchViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    innerPadding: PaddingValues,
    chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory(LocalContext.current.applicationContext as Application)),
    adminViewModel: AdminViewModel = viewModel(factory = AdminViewModel.Factory(LocalContext.current.applicationContext as Application))
) {
    val repository = remember { ChatAuthRepository() }
    val scope = rememberCoroutineScope()
    val adminUser by adminViewModel.user.collectAsState()
    val signedIn by chatViewModel.signedIn.collectAsState()
    val churchViewModel: ChurchViewModel = viewModel()
    val churchInfo by churchViewModel.churchInfo.collectAsState()

    if (!signedIn && adminUser == null) {
        UnifiedAuthScreen(
            churchInfo = churchInfo,
            chatViewModel = chatViewModel,
            adminViewModel = adminViewModel,
            innerPadding = innerPadding,
            onMemberSignedIn = chatViewModel::initChat,
            onAdminLoginSuccess = { /* Already handled in MainActivity */ }
        )
        return
    }
    
    var profile by remember { mutableStateOf<ChatProfile?>(null) }
    var username by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf("") }
    var isAdminVisible by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf(repository.currentEmail().orEmpty()) }
    var newEmail by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var updatingEmail by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        if (signedIn) {
            loading = true
            repository.getProfile()
                .onSuccess { p ->
                    profile = p
                    username = p?.username.orEmpty()
                    avatarUrl = p?.avatar_url.orEmpty()
                    isAdminVisible = p?.is_admin_visible ?: false
                }
            email = repository.currentEmail().orEmpty()
            loading = false
        } else {
            loading = false
        }
    }

    LaunchedEffect(signedIn) { load() }

    Box(Modifier.fillMaxSize().padding(innerPadding)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Header
            ProfileHeader(
                displayName = adminUser?.username ?: username.ifBlank { email.substringBefore("@") },
                avatarUrl = avatarUrl,
                subtitle = if (adminUser != null) "Administrator Account" else "Community Member"
            )

            Spacer(Modifier.height(24.dp))

            // Administrator Info Section
            if (adminUser != null) {
                InfoSection(title = "Administrative Identity", icon = Icons.Default.AdminPanelSettings) {
                    InfoRow(label = "Role", value = adminUser!!.role.replace("_", " ").uppercase())
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Admin Badge in Chat", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Indicate your role when sending messages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isAdminVisible,
                            onCheckedChange = { isAdminVisible = it }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Chat Profile Section
            if (signedIn) {
                InfoSection(title = "Community Identity", icon = Icons.Default.Chat) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.lowercase().removePrefix("@") },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Community Username") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, null, modifier = Modifier.size(18.dp)) },
                        supportingText = { Text("Visible to other church members") }
                    )
                    
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = avatarUrl,
                        onValueChange = { avatarUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Profile Picture URL") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp)) },
                        supportingText = { Text("Direct link to your avatar image") }
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            scope.launch {
                                saving = true
                                message = null
                                error = null
                                val result = repository.completeProfile(
                                    username = username,
                                    avatarUrl = avatarUrl,
                                    adminRole = adminUser?.role,
                                    isAdminVisible = isAdminVisible
                                )
                                if (result.success) {
                                    message = "Profile updated successfully."
                                    repository.getProfile().onSuccess { profile = it }
                                } else {
                                    error = result.message ?: "Unable to save profile changes."
                                }
                                saving = false
                            }
                        },
                        enabled = !saving && username.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Save Profile Changes")
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Account Email Section
                InfoSection(title = "Account Credentials", icon = Icons.Default.Email) {
                    Text("Your current email is: $email", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("New Email Address") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, null, modifier = Modifier.size(18.dp)) },
                        supportingText = { Text("Used for password recovery and sign-in") }
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                updatingEmail = true
                                message = null
                                error = null
                                val result = repository.updateEmail(newEmail)
                                if (result.success) {
                                    message = result.message
                                    newEmail = ""
                                } else {
                                    error = result.message
                                }
                                updatingEmail = false
                            }
                        },
                        enabled = !updatingEmail && newEmail.contains("@") && newEmail.contains("."),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        if (updatingEmail) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Update Account Email")
                    }
                }
            } else if (adminUser == null) {
                // Not signed in to either
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AccountCircle, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text("Sign in to your community account to manage your profile and participate in church chats.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Actions Section
            InfoSection(title = "Security & Sessions", icon = Icons.Default.Settings) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            chatViewModel.signOut()
                            adminViewModel.logout()
                            message = "You have been signed out."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out From All Accounts")
                }
            }

            message?.let {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(it, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }
            error?.let {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ProfileHeader(displayName: String, avatarUrl: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier.size(30.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 2.dp
            ) {
                Icon(Icons.Default.PhotoCamera, null, Modifier.padding(6.dp).size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}
