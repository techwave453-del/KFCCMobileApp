package com.example.helloworld

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.admin.AdminShell
import com.example.helloworld.admin.AdminViewModel
import com.example.helloworld.data.LocalCache
import com.example.helloworld.ui.ChatViewModel
import com.example.helloworld.ui.ChurchViewModel
import com.example.helloworld.ui.PreferencesViewModel
import com.example.helloworld.ui.screens.*
import com.example.helloworld.ui.theme.KFCCTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        LocalCache.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            val prefsViewModel: PreferencesViewModel = viewModel()
            val isDarkMode by prefsViewModel.isDarkMode.collectAsState()
            KFCCTheme(darkTheme = isDarkMode) { KFCCApp() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KFCCApp(
    viewModel: ChurchViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel(factory = AdminViewModel.Factory(LocalContext.current.applicationContext as Application))
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    val churchInfo by viewModel.churchInfo.collectAsState()
    val mediaItems by viewModel.mediaItems.collectAsState()
    val events by viewModel.events.collectAsState()
    val chatSignedIn by chatViewModel.signedIn.collectAsState()
    val adminUser by adminViewModel.user.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    LaunchedEffect(drawerOpen) { if (drawerOpen) drawerState.open() else drawerState.close() }
    LaunchedEffect(drawerState.currentValue) { drawerOpen = drawerState.isOpen }

    fun navigate(destination: AppDestinations) {
        currentDestination = destination
        drawerOpen = false
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("KFCC Menu", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { scope.launch { drawerState.close() } }) { Icon(Icons.Default.Close, "Close menu") }
                }
                HorizontalDivider()
                NavigationDrawerItem(label = { Text("Preferences") }, selected = currentDestination == AppDestinations.PREFERENCES, onClick = { navigate(AppDestinations.PREFERENCES) }, icon = { Icon(Icons.Default.Tune, null) })
                NavigationDrawerItem(label = { Text("Settings") }, selected = currentDestination == AppDestinations.SETTINGS, onClick = { navigate(AppDestinations.SETTINGS) }, icon = { Icon(Icons.Default.Settings, null) })
                NavigationDrawerItem(label = { Text("Version") }, selected = currentDestination == AppDestinations.VERSION, onClick = { navigate(AppDestinations.VERSION) }, icon = { Icon(Icons.Default.Info, null) })
                if (adminUser != null) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(label = { Text("Administration") }, selected = currentDestination == AppDestinations.ADMIN, onClick = { navigate(AppDestinations.ADMIN) }, icon = { Icon(Icons.Default.AdminPanelSettings, null) })
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                val signedIn = chatSignedIn || adminUser != null
                NavigationDrawerItem(
                    label = { Text(if (signedIn) "Sign out" else "Sign in") },
                    selected = false,
                    onClick = {
                        if (signedIn) {
                            if (chatSignedIn) chatViewModel.signOut()
                            if (adminUser != null) adminViewModel.logout()
                            scope.launch { drawerState.close() }
                        } else navigate(AppDestinations.ACCOUNT)
                    },
                    icon = { Icon(if (signedIn) Icons.AutoMirrored.Filled.Logout else Icons.Default.Login, null) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(churchInfo.churchName.ifBlank { "KFCC" }) },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, "Open menu") } }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(currentDestination == AppDestinations.HOME, { navigate(AppDestinations.HOME) }, { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") })
                    NavigationBarItem(currentDestination == AppDestinations.SEARCH, { navigate(AppDestinations.SEARCH) }, { Icon(Icons.Default.Search, "Search") }, label = { Text("Search") })
                    NavigationBarItem(currentDestination == AppDestinations.CHAT, { navigate(AppDestinations.CHAT) }, { Icon(Icons.Default.Chat, "Chat") }, label = { Text("Chat") })
                    NavigationBarItem(selected = currentDestination == AppDestinations.PROFILE || currentDestination == AppDestinations.ACCOUNT, onClick = { navigate(if (chatSignedIn) AppDestinations.PROFILE else AppDestinations.ACCOUNT) }, icon = { Icon(Icons.Default.AccountCircle, "Profile") }, label = { Text("Profile") })
                }
            }
        ) { innerPadding ->
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                when (currentDestination) {
                    AppDestinations.HOME -> HomeScreen(info = churchInfo, mediaItems = mediaItems, events = events, innerPadding = innerPadding, onOpenChat = { navigate(AppDestinations.CHAT) }, onOpenMedia = { navigate(AppDestinations.MEDIA) }, onOpenEvents = { navigate(AppDestinations.EVENTS) }, onOpenGiving = { navigate(AppDestinations.GIVING) }, onOpenSermons = { navigate(AppDestinations.MEDIA) }, onOpenLive = { navigate(AppDestinations.MEDIA) })
                    AppDestinations.EVENTS -> EventsScreen(events, innerPadding)
                    AppDestinations.MEDIA -> MediaScreen(mediaItems, churchInfo.liveStream, innerPadding)
                    AppDestinations.CHAT -> ChatScreen(innerPadding, viewModel = chatViewModel, adminViewModel = adminViewModel, onAdminLoginSuccess = { navigate(AppDestinations.ADMIN) })
                    AppDestinations.ACCOUNT -> ChatScreen(innerPadding, viewModel = chatViewModel, adminViewModel = adminViewModel, onAdminLoginSuccess = { navigate(AppDestinations.ADMIN) })
                    AppDestinations.SEARCH -> SearchScreen(innerPadding)
                    AppDestinations.PROFILE -> ProfileScreen(innerPadding)
                    AppDestinations.NOTIFICATIONS -> NotificationsScreen(innerPadding)
                    AppDestinations.PREFERENCES -> PreferencesScreen(innerPadding)
                    AppDestinations.SETTINGS -> SettingsScreen(innerPadding)
                    AppDestinations.VERSION -> VersionScreen(innerPadding)
                    AppDestinations.GIVING -> GivingScreen(innerPadding)
                    AppDestinations.ADMIN -> AdminShell(adminViewModel)
                }
            }
        }
    }
}

enum class AppDestinations(val label: String) {
    HOME("Home"), EVENTS("Events"), MEDIA("Media"), CHAT("Chat"), ACCOUNT("Account"), SEARCH("Search"), PROFILE("Profile"), NOTIFICATIONS("Notifications"), PREFERENCES("Preferences"), SETTINGS("Settings"), VERSION("Version"), GIVING("Giving"), ADMIN("Admin")
}
