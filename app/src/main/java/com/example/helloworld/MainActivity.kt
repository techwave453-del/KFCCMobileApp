package com.example.helloworld

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.helloworld.data.AppPreferences
import com.example.helloworld.ui.ChurchViewModel
import com.example.helloworld.ui.screens.*
import com.example.helloworld.ui.theme.KFCCTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KFCCRoot() }
    }
}

@Composable
private fun KFCCRoot() {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context.applicationContext) }
    var theme by remember { mutableStateOf(preferences.theme) }
    val darkTheme = when (theme) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemInDarkTheme()
    }

    KFCCTheme(darkTheme = darkTheme) {
        KFCCApp(
            preferences = preferences,
            theme = theme,
            onThemeChanged = { theme = it }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KFCCApp(
    viewModel: ChurchViewModel = viewModel(),
    preferences: AppPreferences,
    theme: String,
    onThemeChanged: (String) -> Unit,
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val churchInfo by viewModel.churchInfo.collectAsState()
    val mediaItems by viewModel.mediaItems.collectAsState()
    val events by viewModel.events.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val openDestination: (AppDestinations) -> Unit = { destination ->
        currentDestination = destination
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(18.dp))
                Text(
                    churchInfo.churchName.ifBlank { "Kingdom Fellowship" },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Home") }, selected = currentDestination == AppDestinations.HOME,
                    onClick = { openDestination(AppDestinations.HOME) },
                    icon = { Icon(Icons.Default.Home, null) }
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    label = { Text("Notifications") }, selected = currentDestination == AppDestinations.NOTIFICATIONS,
                    onClick = { openDestination(AppDestinations.NOTIFICATIONS) },
                    icon = { Icon(Icons.Default.Notifications, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Preferences") }, selected = currentDestination == AppDestinations.PREFERENCES,
                    onClick = { openDestination(AppDestinations.PREFERENCES) },
                    icon = { Icon(Icons.Default.Tune, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") }, selected = currentDestination == AppDestinations.SETTINGS,
                    onClick = { openDestination(AppDestinations.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Version & About") }, selected = currentDestination == AppDestinations.ABOUT,
                    onClick = { openDestination(AppDestinations.ABOUT) },
                    icon = { Icon(Icons.Default.Info, null) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentDestination.label) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentDestination == AppDestinations.PROFILE,
                        onClick = { currentDestination = AppDestinations.PROFILE },
                        icon = { Icon(Icons.Default.Person, "Profile") },
                        label = { Text("Profile") }
                    )
                    NavigationBarItem(
                        selected = currentDestination == AppDestinations.SEARCH,
                        onClick = { currentDestination = AppDestinations.SEARCH },
                        icon = { Icon(Icons.Default.Search, "Search") },
                        label = { Text("Search") }
                    )
                    NavigationBarItem(
                        selected = currentDestination == AppDestinations.CHAT,
                        onClick = { currentDestination = AppDestinations.CHAT },
                        icon = { Icon(Icons.Default.Chat, "Chat") },
                        label = { Text("Chat") }
                    )
                }
            }
        ) { innerPadding ->
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                when (currentDestination) {
                    AppDestinations.HOME -> HomeScreen(churchInfo, events, innerPadding) { action ->
                        currentDestination = when (action) {
                            "Services" -> AppDestinations.SERVICES
                            "Sermons" -> AppDestinations.SERMONS
                            "Giving" -> AppDestinations.GIVING
                            "Events" -> AppDestinations.EVENTS
                            else -> AppDestinations.HOME
                        }
                    }
                    AppDestinations.SERVICES -> ServicesScreen(churchInfo.services, innerPadding)
                    AppDestinations.SERMONS -> MediaScreen(mediaItems, churchInfo.liveStream, innerPadding, title = "Sermons", sermonsOnly = true)
                    AppDestinations.GIVING -> GivingScreen(innerPadding)
                    AppDestinations.EVENTS -> EventsScreen(events, innerPadding)
                    AppDestinations.MEDIA -> MediaScreen(mediaItems, churchInfo.liveStream, innerPadding)
                    AppDestinations.SEARCH -> SearchScreen(churchInfo, events, innerPadding)
                    AppDestinations.CHAT -> ChatScreen(innerPadding)
                    AppDestinations.NOTIFICATIONS -> NotificationsScreen(innerPadding)
                    AppDestinations.PROFILE -> ProfileScreen(innerPadding)
                    AppDestinations.PREFERENCES -> PreferencesScreen(innerPadding, preferences, theme, onThemeChanged)
                    AppDestinations.SETTINGS -> SettingsScreen(innerPadding)
                    AppDestinations.ABOUT -> AboutScreen(innerPadding)
                }
            }
        }
    }
}

enum class AppDestinations(val label: String) {
    HOME("Home"),
    SERVICES("Services"),
    SERMONS("Sermons"),
    GIVING("Giving"),
    EVENTS("Events"),
    MEDIA("Media"),
    SEARCH("Search"),
    CHAT("Chat"),
    NOTIFICATIONS("Notifications"),
    PROFILE("Profile"),
    PREFERENCES("Preferences"),
    SETTINGS("Settings"),
    ABOUT("Version & About")
}
