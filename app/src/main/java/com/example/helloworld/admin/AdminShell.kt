package com.example.helloworld.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.helloworld.admin.media.MediaCenterScreen

@Composable
fun AdminShell(viewModel: AdminViewModel, modifier: Modifier = Modifier) {
    val user by viewModel.user.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var mediaCenterOpen by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            loading && user == null -> LoadingAdminScreen()
            user == null -> AdminLoginScreen(
                loading = loading,
                error = error,
                onLogin = viewModel::login,
                onClearError = viewModel::clearError
            )
            mediaCenterOpen && user.hasPermission(AdminPermissions.MEDIA_VIEW) -> Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { mediaCenterOpen = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to admin dashboard")
                    }
                    Text("Admin Dashboard / Media Center", style = MaterialTheme.typography.titleMedium)
                }
                MediaCenterScreen(modifier = Modifier.weight(1f))
            }
            else -> AdminDashboardScreen(
                user = user,
                onLogout = viewModel::logout,
                onMediaCenter = { mediaCenterOpen = true }
            )
        }
    }
}

@Composable
private fun LoadingAdminScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Checking administrator session…")
    }
}

@Composable
private fun AdminLoginScreen(
    loading: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit,
    onClearError: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
        Spacer(Modifier.height(12.dp))
        Text("KFCC Administration", style = MaterialTheme.typography.headlineMedium)
        Text("Secure access to church administration", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = username, onValueChange = { username = it; if (error != null) onClearError() }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it; if (error != null) onClearError() }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = { onLogin(username, password) }, enabled = username.isNotBlank() && password.isNotBlank() && !loading, modifier = Modifier.fillMaxWidth()) {
            if (loading) CircularProgressIndicator(modifier = Modifier.height(20.dp)) else Text("Sign in")
        }
    }
}

private data class AdminModule(val title: String, val description: String, val permission: String, val icon: ImageVector)

@Composable
private fun AdminDashboardScreen(user: AdminUser, onLogout: () -> Unit, onMediaCenter: () -> Unit) {
    val modules = listOf(
        AdminModule("Church Identity", "Church name, official identity and logo", AdminPermissions.IDENTITY_VIEW, Icons.Default.Security),
        AdminModule("Website Content", "Homepage, pages, services, classes and theme", AdminPermissions.SITE_EDIT, Icons.Default.Article),
        AdminModule("Media Center", "Images, videos, audio, URLs and featured media", AdminPermissions.MEDIA_VIEW, Icons.Default.Image),
        AdminModule("Live Streaming", "Live stream controls and moderation", AdminPermissions.LIVE_VIEW, Icons.Default.LiveTv),
        AdminModule("Users & Permissions", "Administrator accounts, approvals and roles", AdminPermissions.USERS_VIEW, Icons.Default.People),
        AdminModule("System Administration", "Security, permissions and audit", AdminPermissions.AUDIT_VIEW, Icons.Default.AdminPanelSettings)
    )

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Admin Dashboard", style = MaterialTheme.typography.headlineSmall)
                Text(if (user.role == "super_admin") "Super Admin · ${user.username}" else "${user.role} · ${user.username}", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, contentDescription = "Sign out") }
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        Text("Administration modules", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(modules) { module ->
                val allowed = user.hasPermission(module.permission)
                Card(modifier = Modifier.fillMaxWidth().then(if (allowed && module.title == "Media Center") Modifier.clickable(onClick = onMediaCenter) else Modifier)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(module.icon, contentDescription = null)
                        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(module.title, style = MaterialTheme.typography.titleMedium)
                            Text(module.description, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            Text(if (allowed) "Access available" else "Permission not assigned", color = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
