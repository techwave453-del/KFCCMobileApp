package com.example.helloworld.admin

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.admin.content.WebsiteContentScreen
import com.example.helloworld.admin.events.AdminEventsScreen
import com.example.helloworld.admin.events.AdminEventsViewModel
import com.example.helloworld.admin.identity.ChurchIdentityScreen
import com.example.helloworld.admin.live.LiveStreamingScreen
import com.example.helloworld.admin.media.MediaCenterScreen
import com.example.helloworld.admin.users.AdminUsersScreen
import com.example.helloworld.admin.users.AdminUsersViewModel

private const val NO_ADMIN_PERMISSIONS = "Your administrator account has been created, but no administration permissions have been assigned yet."

@Composable
fun AdminShell(viewModel: AdminViewModel, modifier: Modifier = Modifier) {
    val user by viewModel.user.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var openModule by remember { mutableStateOf<String?>(null) }
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as Application

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            loading && user == null -> LoadingAdminScreen()
            user == null -> AdminLoginScreen(loading, error, viewModel::login, viewModel::clearError)
            else -> user!!.let { currentUser ->
                when {
                    currentUser.permissions.isEmpty() && currentUser.role != "super_admin" -> NoPermissionsScreen(viewModel::logout)
                    openModule == "identity" && currentUser.hasPermission(AdminPermissions.IDENTITY_VIEW) && currentUser.hasPermission(AdminPermissions.IDENTITY_EDIT) -> ModuleFrame("Church Identity", { openModule = null }) { ChurchIdentityScreen(Modifier.fillMaxSize()) }
                    openModule == "content" && currentUser.hasPermission(AdminPermissions.SITE_EDIT) -> ModuleFrame("Website Content", { openModule = null }) { WebsiteContentScreen(Modifier.fillMaxSize()) }
                    openModule == "events" && currentUser.hasPermission(AdminPermissions.SITE_EDIT) -> ModuleFrame("Events Management", { openModule = null }) { AdminEventsScreen(Modifier.fillMaxSize(), viewModel(factory = AdminEventsViewModel.Factory(application))) }
                    openModule == "live" && currentUser.hasPermission(AdminPermissions.LIVE_MANAGE) -> ModuleFrame("Live Streaming", { openModule = null }) { LiveStreamingScreen(Modifier.fillMaxSize()) }
                    openModule == "media" && currentUser.hasPermission(AdminPermissions.MEDIA_VIEW) -> ModuleFrame("Media Center", { openModule = null }) { MediaCenterScreen(Modifier.fillMaxSize(), canUpload = currentUser.hasPermission(AdminPermissions.MEDIA_UPLOAD), canEdit = currentUser.hasPermission(AdminPermissions.MEDIA_EDIT), canDelete = currentUser.hasPermission(AdminPermissions.MEDIA_DELETE)) }
                    openModule == "users" && currentUser.hasPermission(AdminPermissions.USERS_VIEW) -> ModuleFrame("Users & Permissions", { openModule = null }) { AdminUsersScreen(Modifier.fillMaxSize(), viewModel(factory = AdminUsersViewModel.Factory(application)), { openModule = null }) }
                    else -> AdminDashboardScreen(currentUser, viewModel::logout, { openModule = "identity" }, { openModule = "content" }, { openModule = "events" }, { openModule = "live" }, { openModule = "media" }, { openModule = "users" })
                }
            }
        }
    }
}

@Composable
private fun ModuleFrame(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Column { Text("Administration", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary); Text(title, style = MaterialTheme.typography.titleLarge) }
            }
        }
        content()
    }
}

@Composable
private fun LoadingAdminScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Spacer(Modifier.height(14.dp)); Text("Checking administrator session…", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
}

@Composable
private fun NoPermissionsScreen(onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(Modifier.size(76.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Security, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary) } }
        Spacer(Modifier.height(20.dp)); Text("Administration access pending", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.height(10.dp)); Text(NO_ADMIN_PERMISSIONS, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(22.dp)); OutlinedButton(onClick = onLogout) { Text("Sign out") }
    }
}

@Composable
private fun AdminLoginScreen(loading: Boolean, error: String?, onLogin: (String, String) -> Unit, onClearError: () -> Unit) {
    var username by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Surface(Modifier.size(68.dp), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AdminPanelSettings, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary) } }
        Spacer(Modifier.height(16.dp)); Text("KFCC Administration", style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(5.dp)); Text("Secure access to church administration", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(26.dp))
        OutlinedTextField(username, { username = it; if (error != null) onClearError() }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Spacer(Modifier.height(12.dp)); OutlinedTextField(password, { password = it; if (error != null) onClearError() }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        if (error != null) { Spacer(Modifier.height(12.dp)); Text(error, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(20.dp)); Button(onClick = { onLogin(username, password) }, enabled = username.isNotBlank() && password.isNotBlank() && !loading, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { if (loading) CircularProgressIndicator(Modifier.size(20.dp)) else Text("Sign in", fontWeight = FontWeight.Bold) }
    }
}

private data class AdminModule(val title: String, val description: String, val permission: String, val icon: ImageVector)

@Composable
private fun AdminDashboardScreen(user: AdminUser, onLogout: () -> Unit, onIdentity: () -> Unit, onContent: () -> Unit, onEvents: () -> Unit, onLive: () -> Unit, onMedia: () -> Unit, onUsers: () -> Unit) {
    val modules = listOf(
        AdminModule("Church Identity", "Church name, official identity and logo", AdminPermissions.IDENTITY_VIEW, Icons.Default.Security),
        AdminModule("Website Content", "Homepage, pages, services, classes and theme", AdminPermissions.SITE_EDIT, Icons.Default.Article),
        AdminModule("Events Management", "Create, publish, feature and maintain church events", AdminPermissions.SITE_EDIT, Icons.Default.Event),
        AdminModule("Live Streaming", "Manage broadcasts and the public live message", AdminPermissions.LIVE_MANAGE, Icons.Default.LiveTv),
        AdminModule("Media Center", "Images, videos, audio, URLs and featured media", AdminPermissions.MEDIA_VIEW, Icons.Default.Image),
        AdminModule("Users & Permissions", "Administrator accounts, approvals and roles", AdminPermissions.USERS_VIEW, Icons.Default.People),
        AdminModule("System Administration", "Security, permissions and audit", AdminPermissions.AUDIT_VIEW, Icons.Default.AdminPanelSettings)
    )
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Admin Dashboard", style = MaterialTheme.typography.headlineSmall); Text(if (user.role == "super_admin") "Super Admin · ${user.username}" else "${user.role} · ${user.username}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Sign out") }
        }
        Text("Administration", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        LazyColumn(contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(modules) { module ->
                val allowed = user.hasPermission(module.permission) && (module.title != "Church Identity" || user.hasPermission(AdminPermissions.IDENTITY_EDIT))
                val action = when (module.title) { "Church Identity" -> if (allowed) onIdentity else null; "Website Content" -> if (allowed) onContent else null; "Events Management" -> if (allowed) onEvents else null; "Live Streaming" -> if (allowed) onLive else null; "Media Center" -> if (allowed) onMedia else null; "Users & Permissions" -> if (allowed) onUsers else null; else -> null }
                Card(Modifier.fillMaxWidth().then(if (action != null) Modifier.clickable(onClick = action) else Modifier), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(48.dp), shape = RoundedCornerShape(15.dp), color = if (allowed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) { Box(contentAlignment = Alignment.Center) { Icon(module.icon, null, tint = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) } }
                        Column(Modifier.weight(1f).padding(start = 14.dp)) { Text(module.title, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(3.dp)); Text(module.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(5.dp)); Text(if (allowed) "Access available" else "Permission not assigned", color = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) }
                        if (action != null) Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
