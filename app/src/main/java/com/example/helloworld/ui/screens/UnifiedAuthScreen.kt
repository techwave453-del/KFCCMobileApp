package com.example.helloworld.ui.screens

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.helloworld.admin.AdminRepositoryProvider
import com.example.helloworld.admin.AdminViewModel
import com.example.helloworld.auth.UnifiedAuthRepository
import com.example.helloworld.auth.UnifiedAuthResult
import com.example.helloworld.data.ChatAuthRepository
import com.example.helloworld.data.ChurchInfo
import com.example.helloworld.ui.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun UnifiedAuthScreen(
    churchInfo: ChurchInfo,
    chatViewModel: ChatViewModel,
    adminViewModel: AdminViewModel,
    innerPadding: PaddingValues,
    onMemberSignedIn: () -> Unit,
    onAdminLoginSuccess: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember(context) {
        UnifiedAuthRepository(
            AdminRepositoryProvider.get(context.applicationContext as Application),
            ChatAuthRepository()
        )
    }
    var register by rememberSaveable { mutableStateOf(false) }
    var identifier by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var verificationPending by rememberSaveable { mutableStateOf(false) }

    val churchName = churchInfo.churchName.ifBlank { "Welcome" }
    val tagline = churchInfo.tagline.ifBlank { "Growing together in faith, hope and love" }

    Surface(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxWidth().height(160.dp)
                    .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)))
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (churchInfo.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = churchInfo.logoUrl,
                        contentDescription = "$churchName logo",
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(16.dp).size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(churchName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onPrimary)
                Text(tagline, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
                Spacer(Modifier.height(20.dp))

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text(if (register) "Create Account" else "Sign In", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(if (register) "Join your church community and stay connected." else "Welcome back. Sign in to continue.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(20.dp))

                        if (register) {
                            OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Username") }, leadingIcon = { Icon(Icons.Default.Person, null) }, singleLine = true)
                            Spacer(Modifier.height(12.dp))
                        }
                        OutlinedTextField(identifier, { identifier = it }, Modifier.fillMaxWidth(), label = { Text("Email or phone number") }, leadingIcon = { Icon(Icons.Default.Email, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, trailingIcon = { IconButton({ showPassword = !showPassword }) { Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Toggle password visibility") } }, visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
                        if (register) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(confirmPassword, { confirmPassword = it }, Modifier.fillMaxWidth(), label = { Text("Confirm password") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
                        }

                        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp)) }
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                error = null
                                if (register && password != confirmPassword) { error = "Passwords do not match."; return@Button }
                                loading = true
                                scope.launch {
                                    try {
                                        if (register) {
                                            val result = ChatAuthRepository().signUp(identifier, password, username)
                                            if (result.success) verificationPending = true else error = result.message ?: "Unable to create your account."
                                        } else {
                                            when (val result = authRepository.signIn(identifier.trim(), password)) {
                                                is UnifiedAuthResult.Administrator -> {
                                                    // Ensure the administrator has a community chat identity
                                                    try {
                                                        ChatAuthRepository().completeProfile(
                                                            username = result.username,
                                                            adminRole = "super_admin" // Initial default
                                                        )
                                                    } catch (_: Exception) {}
                                                    
                                                    // The AdminViewModel is created before the login session exists.
                                                    // Explicitly restore it after admin-login imports the Supabase session
                                                    // so Profile and Administration immediately see the same admin identity.
                                                    adminViewModel.restoreSession()
                                                    chatViewModel.onSignedIn()
                                                    onAdminLoginSuccess()
                                                }
                                                is UnifiedAuthResult.Member -> { 
                                                    chatViewModel.onSignedIn()
                                                    onMemberSignedIn() 
                                                }
                                            }
                                        }
                                    } catch (_: Exception) { error = "Unable to complete sign in. Please try again." }
                                    loading = false
                                }
                            },
                            enabled = !loading && identifier.isNotBlank() && password.isNotBlank() && (!register || (username.isNotBlank() && confirmPassword.isNotBlank())),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Text(if (register) "Create Account" else "Sign In") }

                        if (verificationPending) {
                            Spacer(Modifier.height(12.dp))
                            Text("Check your email to verify your account, then return and sign in.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        if (!register) TextButton(onClick = { error = "Password reset will be connected to the Supabase recovery flow." }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Forgot password?") }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (register) "Already have an account?" else "Don't have an account?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { register = !register; error = null; verificationPending = false }) { Text(if (register) "Sign in" else "Create account") }
                }
            }
        }
    }
}
