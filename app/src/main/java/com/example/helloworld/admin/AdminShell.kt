package com.example.helloworld.admin

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.admin.content.WebsiteContentScreen
import com.example.helloworld.admin.events.AdminEventsScreen
import com.example.helloworld.admin.events.AdminEventsViewModel
import com.example.helloworld.admin.identity.ChurchIdentityScreen
import com.example.helloworld.admin.live.LiveStreamingScreen
import com.example.helloworld.admin.media.MediaCenterScreen
import com.example.helloworld.admin.services.AdminServicesScreen
import com.example.helloworld.admin.system.SystemAdministrationScreen
import com.example.helloworld.admin.content.WebsiteContentViewModel
import com.example.helloworld.admin.users.AdminUsersScreen
import com.example.helloworld.admin.users.AdminUsersViewModel

private const val NO_ADMIN_PERMISSIONS = "Your administrator account has been created, but no administration permissions have been assigned yet."

@Composable
fun AdminShell(
    viewModel: AdminViewModel,
    innerPadding: PaddingValues,
    onBackToApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user by viewModel.user.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    var openModule by remember { mutableStateOf<String?>(null) }
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as Application

    Surface(modifier = Modifier.fillMaxSize().padding(innerPadding), color = MaterialTheme.colorScheme.background) {
        when {
            loading && user == null -> LoadingAdminScreen()
            user == null -> AdminAccessRequiredScreen(onBackToApp)
            else -> user!!.let { currentUser ->
                when {
                    currentUser.permissions.isEmpty() && currentUser.role != "super_admin" -> NoPermissionsScreen(viewModel::logout)
                    openModule == "credentials" ->
                        ModuleFrame("Account Credentials", { openModule = null }) { AccountCredentialsScreen(currentUser, Modifier.fillMaxSize()) }
                    openModule == "identity" && currentUser.hasPermission(AdminPermissions.IDENTITY_VIEW) && currentUser.hasPermission(AdminPermissions.IDENTITY_EDIT) ->
                        ModuleFrame("Church Identity", { openModule = null }) { ChurchIdentityScreen(Modifier.fillMaxSize()) }
                    openModule == "content" && currentUser.hasPermission(AdminPermissions.SITE_EDIT) ->
                        ModuleFrame("Website Content", { openModule = null }) {
                            WebsiteContentScreen(
                                modifier = Modifier.fillMaxSize(),
                                canEditIdentity = currentUser.hasPermission(AdminPermissions.IDENTITY_EDIT),
                                canManageLive = currentUser.hasPermission(AdminPermissions.LIVE_MANAGE)
                            )
                        }
                    openModule == "services" && currentUser.hasPermission(AdminPermissions.SITE_EDIT) ->
                        ModuleFrame("Services & Giving", { openModule = null }) { AdminServicesScreen(Modifier.fillMaxSize(), viewModel(factory = WebsiteContentViewModel.Factory(application))) }
                    openModule == "events" && currentUser.hasPermission(AdminPermissions.SITE_EDIT) ->
                        ModuleFrame("Events Management", { openModule = null }) { AdminEventsScreen(Modifier.fillMaxSize(), viewModel(factory = AdminEventsViewModel.Factory(application))) }
                    openModule == "live" && currentUser.hasPermission(AdminPermissions.LIVE_MANAGE) ->
                        ModuleFrame("Live Streaming", { openModule = null }) { LiveStreamingScreen(Modifier.fillMaxSize()) }
                    openModule == "media" && currentUser.hasPermission(AdminPermissions.MEDIA_VIEW) ->
                        ModuleFrame("Media Center", { openModule = null }) {
                            MediaCenterScreen(
                                Modifier.fillMaxSize(),
                                canUpload = currentUser.hasPermission(AdminPermissions.MEDIA_UPLOAD),
                                canEdit = currentUser.hasPermission(AdminPermissions.MEDIA_EDIT),
                                canDelete = currentUser.hasPermission(AdminPermissions.MEDIA_DELETE)
                            )
                        }
                    openModule == "users" && currentUser.hasPermission(AdminPermissions.USERS_VIEW) ->
                        ModuleFrame("Users & Permissions", { openModule = null }) { AdminUsersScreen(Modifier.fillMaxSize(), viewModel(factory = AdminUsersViewModel.Factory(application)), { openModule = null }) }
                    openModule == "system" && currentUser.hasPermission(AdminPermissions.AUDIT_VIEW) ->
                        ModuleFrame("System Administration", { openModule = null }) { SystemAdministrationScreen(Modifier.fillMaxSize()) }
                    else -> AdminDashboardScreen(
                        currentUser,
                        viewModel::logout,
                        { openModule = "credentials" },
                        { openModule = "identity" },
                        { openModule = "content" },
                        { openModule = "services" },
                        { openModule = "events" },
                        { openModule = "live" },
                        { openModule = "media" },
                        { openModule = "users" },
                        { openModule = "system" }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleFrame(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Admin Dashboard / $title", style = MaterialTheme.typography.titleMedium)
        }
        content()
    }
}

@Composable
private fun LoadingAdminScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Checking administrator session…")
    }
}

@Composable
private fun AdminAccessRequiredScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.AdminPanelSettings, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Administration access required", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Sign in through the unified account screen with an administrator account to access the dashboard.", textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, null)
            Spacer(Modifier.width(8.dp))
            Text("Back to App")
        }
    }
}

@Composable
private fun NoPermissionsScreen(onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Security, null)
        Spacer(Modifier.height(16.dp))
        Text("Administration access pending", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(NO_ADMIN_PERMISSIONS)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onLogout) { Text("Sign out") }
    }
}

private data class AdminModule(val title: String, val description: String, val permission: String, val icon: ImageVector)

@Composable
private fun AdminDashboardScreen(
    user: AdminUser,
    onLogout: () -> Unit,
    onCredentials: () -> Unit,
    onIdentity: () -> Unit,
    onContent: () -> Unit,
    onServices: () -> Unit,
    onEvents: () -> Unit,
    onLive: () -> Unit,
    onMedia: () -> Unit,
    onUsers: () -> Unit,
    onSystem: () -> Unit,
    onBackToApp: () -> Unit
) {
    val modules = listOf(
        AdminModule("Account Credentials", "Administrator username, email, role and status", "account.credentials.view", Icons.Default.AccountCircle),
        AdminModule("Church Identity", "Church name, official identity and logo", AdminPermissions.IDENTITY_VIEW, Icons.Default.Security),
        AdminModule("Website Content", "Homepage, pages, classes and theme", AdminPermissions.SITE_EDIT, Icons.Default.Article),
        AdminModule("Services & Giving", "Manage worship times and online giving links", AdminPermissions.SITE_EDIT, Icons.Default.Church),
        AdminModule("Events Management", "Create, publish, feature and maintain church events", AdminPermissions.SITE_EDIT, Icons.Default.Event),
        AdminModule("Live Streaming", "Enable broadcasts, manage the stream URL and public live message", AdminPermissions.LIVE_MANAGE, Icons.Default.LiveTv),
        AdminModule("Media Center", "Images, videos, audio, URLs and featured media", AdminPermissions.MEDIA_VIEW, Icons.Default.Image),
        AdminModule("Users & Permissions", "Administrator accounts, approvals and roles", AdminPermissions.USERS_VIEW, Icons.Default.People),
        AdminModule("System Administration", "Security, permissions and audit", AdminPermissions.AUDIT_VIEW, Icons.Default.AdminPanelSettings)
    )

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackToApp) { Icon(Icons.Default.ArrowBack, "Back to App") }
            Column(Modifier.weight(1f)) {
                Text("Admin Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(if (user.role == "super_admin") "Super Admin · ${user.username}" else "${user.role} · ${user.username}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Sign out", tint = MaterialTheme.colorScheme.error) }
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        Text("Administration modules", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(modules) { module ->
                val allowed = user.hasPermission(module.permission) &&
                    (module.title != "Church Identity" || user.hasPermission(AdminPermissions.IDENTITY_EDIT))
                val action = when (module.title) {
                    "Account Credentials" -> if (allowed) onCredentials else null
                    "Church Identity" -> if (allowed) onIdentity else null
                    "Website Content" -> if (allowed) onContent else null
                    "Services & Giving" -> if (allowed) onServices else null
                    "Events Management" -> if (allowed) onEvents else null
                    "Live Streaming" -> if (allowed) onLive else null
                    "Media Center" -> if (allowed) onMedia else null
                    "Users & Permissions" -> if (allowed) onUsers else null
                    "System Administration" -> if (allowed) onSystem else null
                    else -> null
                }
                Card(Modifier.fillMaxWidth().then(if (action != null) Modifier.clickable(onClick = action) else Modifier)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(module.icon, null)
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
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
