package com.example.helloworld.admin

import android.app.Application
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.helloworld.admin.content.WebsiteContentViewModel
import com.example.helloworld.admin.users.AdminUsersUnifiedScreen
import com.example.helloworld.admin.notifications.AdminNotificationsScreen
import com.example.helloworld.admin.updates.AppUpdateAdminScreen
import com.example.helloworld.admin.games.AdminBibleGamesScreen
import com.example.helloworld.admin.backgrounds.ScreenBackgroundsScreen
import com.example.helloworld.ui.utils.WindowSizeClass
import com.example.helloworld.ui.utils.rememberWindowSizeClass

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
    val application = LocalContext.current.applicationContext as Application

    Surface(modifier = Modifier.fillMaxSize().padding(innerPadding), color = MaterialTheme.colorScheme.background) {
        when {
            loading && user == null -> LoadingAdminScreen()
            user == null -> AdminAccessRequiredScreen(onBackToApp)
            else -> user!!.let { currentUser ->
                when {
                    currentUser.permissions.isEmpty() && currentUser.role != "super_admin" -> NoPermissionsScreen(viewModel::logout)
                    openModule == "credentials" ->
                        ModuleFrame("Account Credentials", { openModule = null }) { AccountCredentialsScreen(currentUser, Modifier.fillMaxSize()) }
                    openModule == "identity" && currentUser.canEditChurchIdentity() ->
                        ModuleFrame("Church Identity", { openModule = null }) { ChurchIdentityScreen(Modifier.fillMaxSize()) }
                    openModule == "content" && currentUser.hasPermission(AdminPermissions.SITE_EDIT) ->
                        ModuleFrame("Website Content", { openModule = null }) { WebsiteContentScreen(Modifier.fillMaxSize()) }
                    openModule == "services" && (currentUser.hasPermission(AdminPermissions.SERVICES_EDIT) || currentUser.hasPermission(AdminPermissions.LINKS_EDIT) || currentUser.hasPermission(AdminPermissions.SITE_EDIT)) ->
                        ModuleFrame("Services & Giving", { openModule = null }) {
                            AdminServicesScreen(
                                modifier = Modifier.fillMaxSize(),
                                viewModel = viewModel(factory = WebsiteContentViewModel.Factory(application)),
                                canEditServices = currentUser.hasPermission(AdminPermissions.SERVICES_EDIT) || currentUser.hasPermission(AdminPermissions.SITE_EDIT),
                                canEditGiving = currentUser.hasPermission(AdminPermissions.LINKS_EDIT) || currentUser.hasPermission(AdminPermissions.SITE_EDIT)
                            )
                        }
                    openModule == "events" && currentUser.hasPermission(AdminPermissions.EVENTS_MANAGE) ->
                        ModuleFrame("Events Management", { openModule = null }) { AdminEventsScreen(Modifier.fillMaxSize(), viewModel(factory = AdminEventsViewModel.Factory(application))) }
                    openModule == "notifications" && currentUser.hasPermission(AdminPermissions.NOTIFICATIONS_SEND) ->
                        ModuleFrame("Notifications", { openModule = null }) { AdminNotificationsScreen(Modifier.fillMaxSize()) }
                    openModule == "app_update" && currentUser.hasPermission(AdminPermissions.APP_UPDATE_MANAGE) ->
                        ModuleFrame("App Updates", { openModule = null }) { AppUpdateAdminScreen(Modifier.fillMaxSize()) }
                    openModule == "bible_games" && currentUser.hasPermission(AdminPermissions.BIBLE_GAMES_MANAGE) ->
                        ModuleFrame("Bible Games", { openModule = null }) { AdminBibleGamesScreen(Modifier.fillMaxSize()) }
                    openModule == "daily_scripture" && currentUser.hasPermission(AdminPermissions.DAILY_SCRIPTURE_MANAGE) ->
                        ModuleFrame("Today's Scripture", { openModule = null }) { AdminDailyScriptureScreen(Modifier.fillMaxSize()) }
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
                    openModule == "backgrounds" && (currentUser.hasPermission(AdminPermissions.MEDIA_UPLOAD) || currentUser.hasPermission(AdminPermissions.MEDIA_EDIT)) ->
                        ModuleFrame("Screen Backgrounds", { openModule = null }) {
                            ScreenBackgroundsScreen(
                                Modifier.fillMaxSize(),
                                canUpload = currentUser.hasPermission(AdminPermissions.MEDIA_UPLOAD),
                                canEdit = currentUser.hasPermission(AdminPermissions.MEDIA_EDIT)
                            )
                        }
                    openModule == "users" && currentUser.hasPermission(AdminPermissions.USERS_VIEW) ->
                        ModuleFrame("Users & Members Management", { openModule = null }) { AdminUsersUnifiedScreen(Modifier.fillMaxSize(), currentUser = currentUser, onBack = { openModule = null }) }
                    openModule == "system" && currentUser.hasPermission(AdminPermissions.AUDIT_VIEW) ->
                        ModuleFrame("System Administration", { openModule = null }) { SystemAdministrationScreen(currentUser, Modifier.fillMaxSize()) }
                    else -> AdminDashboardScreen(
                        currentUser,
                        onBackToApp,
                        viewModel::logout,
                        { openModule = "credentials" },
                        { openModule = "identity" },
                        { openModule = "content" },
                        { openModule = "services" },
                        { openModule = "events" },
                        { openModule = "notifications" },
                        { openModule = "app_update" },
                        { openModule = "bible_games" },
                        { openModule = "daily_scripture" },
                        { openModule = "live" },
                        { openModule = "media" },
                        { openModule = "backgrounds" },
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

private data class AdminModule(val title: String, val description: String, val permission: String, val icon: ImageVector, val category: String)

@Composable
private fun AdminDashboardScreen(
    user: AdminUser,
    onBackToApp: () -> Unit,
    onLogout: () -> Unit,
    onCredentials: () -> Unit,
    onIdentity: () -> Unit,
    onContent: () -> Unit,
    onServices: () -> Unit,
    onEvents: () -> Unit,
    onNotifications: () -> Unit,
    onAppUpdate: () -> Unit,
    onBibleGames: () -> Unit,
    onDailyScripture: () -> Unit,
    onLive: () -> Unit,
    onMedia: () -> Unit,
    onBackgrounds: () -> Unit,
    onUsers: () -> Unit,
    onSystem: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val windowSize = rememberWindowSizeClass()
    val columns = if (windowSize == WindowSizeClass.COMPACT) 1 else 2

    val modules = listOf(
        AdminModule("Account Credentials", "Administrator username, email, role and status", AdminPermissions.ACCOUNT_CREDENTIALS_VIEW, Icons.Default.AccountCircle, "Administration & Security"),
        AdminModule("Church Identity", "Church name, official identity and logo", AdminPermissions.IDENTITY_VIEW, Icons.Default.Security, "Content & Branding"),
        AdminModule("Website Content", "Homepage, pages, classes and theme", AdminPermissions.SITE_EDIT, Icons.Default.Article, "Content & Branding"),
        AdminModule("Services & Giving", "Manage worship times and online giving links", AdminPermissions.SERVICES_EDIT, Icons.Default.Church, "Content & Branding"),
        AdminModule("Events Management", "Create, publish, feature and maintain church events", AdminPermissions.EVENTS_MANAGE, Icons.Default.Event, "Ministry & Outreach"),
        AdminModule("Notifications", "Send church-wide announcements to members", AdminPermissions.NOTIFICATIONS_SEND, Icons.Default.Campaign, "Ministry & Outreach"),
        AdminModule("App Updates", "Configure the latest Android APK and automatic update delivery", AdminPermissions.APP_UPDATE_MANAGE, Icons.Default.SystemUpdate, "Ministry & Outreach"),
        AdminModule("Bible Games", "Create, edit and publish Bible quiz questions", AdminPermissions.BIBLE_GAMES_MANAGE, Icons.Default.SportsEsports, "Ministry & Outreach"),
        AdminModule("Today's Scripture", "Manage themes, Bible references, situations and reflections", AdminPermissions.DAILY_SCRIPTURE_MANAGE, Icons.Default.MenuBook, "Content & Branding"),
        AdminModule("Live Streaming", "Enable broadcasts, manage the stream URL and public live message", AdminPermissions.LIVE_MANAGE, Icons.Default.LiveTv, "Ministry & Outreach"),
        AdminModule("Media Center", "Images, videos, audio, URLs and featured media", AdminPermissions.MEDIA_VIEW, Icons.Default.Image, "Ministry & Outreach"),
        AdminModule("Screen Backgrounds", "Upload and assign backgrounds for app screens", AdminPermissions.MEDIA_UPLOAD, Icons.Default.Wallpaper, "Content & Branding"),
        AdminModule("Users & Members", "Manage app community members, roles, admin accounts & requests", AdminPermissions.USERS_VIEW, Icons.Default.People, "Administration & Security"),
        AdminModule("System Administration", "Security, permissions and audit", AdminPermissions.AUDIT_VIEW, Icons.Default.AdminPanelSettings, "Administration & Security")
    )

    val permittedModules = remember(user, modules) {
        modules.filter { module ->
            when (module.title) {
                "Church Identity" -> user.canEditChurchIdentity()
                "Services & Giving" ->
                    user.hasPermission(AdminPermissions.SERVICES_EDIT) ||
                        user.hasPermission(AdminPermissions.LINKS_EDIT) ||
                        user.hasPermission(AdminPermissions.SITE_EDIT)
                "Screen Backgrounds" ->
                    user.hasPermission(AdminPermissions.MEDIA_UPLOAD) ||
                        user.hasPermission(AdminPermissions.MEDIA_EDIT)
                else -> user.hasPermission(module.permission)
            }
        }
    }

    val filteredModules = remember(searchQuery, permittedModules) {
        if (searchQuery.isBlank()) permittedModules
        else permittedModules.filter { it.title.contains(searchQuery, true) || it.description.contains(searchQuery, true) }
    }

    val groupedModules = filteredModules.groupBy { it.category }

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

        // Quick Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            placeholder = { Text("Search admin modules...") },
            leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null, Modifier.size(18.dp)) } }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            groupedModules.forEach { (category, categoryModules) ->
                item {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(categoryModules.chunked(columns)) { rowModules ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowModules.forEach { module ->
                            Box(modifier = Modifier.weight(1f)) {
                                AdminModuleCard(
                                    module = module,
                                    user = user,
                                    onCredentials = onCredentials,
                                    onIdentity = onIdentity,
                                    onContent = onContent,
                                    onServices = onServices,
                                    onEvents = onEvents,
                                    onNotifications = onNotifications,
                                    onAppUpdate = onAppUpdate,
                                    onBibleGames = onBibleGames,
                                    onDailyScripture = onDailyScripture,
                                    onLive = onLive,
                                    onMedia = onMedia,
                                    onBackgrounds = onBackgrounds,
                                    onUsers = onUsers,
                                    onSystem = onSystem
                                )
                            }
                        }
                        if (rowModules.size < columns && columns > 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminModuleCard(
    module: AdminModule,
    user: AdminUser,
    onCredentials: () -> Unit,
    onIdentity: () -> Unit,
    onContent: () -> Unit,
    onServices: () -> Unit,
    onEvents: () -> Unit,
    onNotifications: () -> Unit,
    onAppUpdate: () -> Unit,
    onBibleGames: () -> Unit,
    onDailyScripture: () -> Unit,
    onLive: () -> Unit,
    onMedia: () -> Unit,
    onBackgrounds: () -> Unit,
    onUsers: () -> Unit,
    onSystem: () -> Unit
) {
    val allowed = when (module.title) {
        "Church Identity" -> user.canEditChurchIdentity()
        "Services & Giving" ->
            user.hasPermission(AdminPermissions.SERVICES_EDIT) ||
                user.hasPermission(AdminPermissions.LINKS_EDIT) ||
                user.hasPermission(AdminPermissions.SITE_EDIT)
        "Screen Backgrounds" ->
            user.hasPermission(AdminPermissions.MEDIA_UPLOAD) ||
                user.hasPermission(AdminPermissions.MEDIA_EDIT)
        else -> user.hasPermission(module.permission)
    }
    val action = when (module.title) {
        "Account Credentials" -> if (allowed) onCredentials else null
        "Church Identity" -> if (allowed) onIdentity else null
        "Website Content" -> if (allowed) onContent else null
        "Services & Giving" -> if (allowed) onServices else null
        "Events Management" -> if (allowed) onEvents else null
        "Notifications" -> if (allowed) onNotifications else null
        "App Updates" -> if (allowed) onAppUpdate else null
        "Bible Games" -> if (allowed) onBibleGames else null
        "Today's Scripture" -> if (allowed) onDailyScripture else null
        "Live Streaming" -> if (allowed) onLive else null
        "Media Center" -> if (allowed) onMedia else null
        "Screen Backgrounds" -> if (allowed) onBackgrounds else null
        "Users & Members" -> if (allowed) onUsers else null
        "System Administration" -> if (allowed) onSystem else null
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth().then(if (action != null) Modifier.clickable(onClick = action) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = if (allowed) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (allowed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (allowed) module.icon else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(module.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(module.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (allowed) "Access available" else "Permission not assigned",
                    color = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
