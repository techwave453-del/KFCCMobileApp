package com.example.helloworld

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.helloworld.data.LocalCache
import com.example.helloworld.ui.ChurchViewModel
import com.example.helloworld.ui.screens.*
import com.example.helloworld.ui.theme.KFCCTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        LocalCache.initialize(applicationContext)
        enableEdgeToEdge()
        setContent { KFCCTheme { KFCCApp() } }
    }
}

@Composable
fun KFCCApp(viewModel: ChurchViewModel = viewModel()) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    val churchInfo by viewModel.churchInfo.collectAsState()
    val mediaItems by viewModel.mediaItems.collectAsState()
    val events by viewModel.events.collectAsState()
    val scope = rememberCoroutineScope()

    fun navigate(destination: AppDestinations) {
        currentDestination = destination
        drawerOpen = false
    }

    ModalNavigationDrawer(
        drawerState = rememberDrawerState(if (drawerOpen) DrawerValue.Open else DrawerValue.Closed),
        drawerContent = {
            ModalDrawerSheet {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("KFCC", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { drawerOpen = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close menu")
                    }
                }
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Home") },
                    selected = currentDestination == AppDestinations.HOME,
                    onClick = { navigate(AppDestinations.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Events") },
                    selected = currentDestination == AppDestinations.EVENTS,
                    onClick = { navigate(AppDestinations.EVENTS) },
                    icon = { Icon(Icons.Default.Event, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Media") },
                    selected = currentDestination == AppDestinations.MEDIA,
                    onClick = { navigate(AppDestinations.MEDIA) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Community Chat") },
                    selected = currentDestination == AppDestinations.CHAT,
                    onClick = { navigate(AppDestinations.CHAT) },
                    icon = { Icon(Icons.Default.Chat, contentDescription = null) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                NavigationDrawerItem(
                    label = { Text("Sign up / Sign in") },
                    selected = currentDestination == AppDestinations.ACCOUNT,
                    onClick = { navigate(AppDestinations.ACCOUNT) },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(churchInfo.name.ifBlank { "KFCC" }) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerOpen = true } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentDestination == AppDestinations.HOME,
                        onClick = { navigate(AppDestinations.HOME) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentDestination == AppDestinations.EVENTS,
                        onClick = { navigate(AppDestinations.EVENTS) },
                        icon = { Icon(Icons.Default.Event, contentDescription = "Events") },
                        label = { Text("Events") }
                    )
                    NavigationBarItem(
                        selected = currentDestination == AppDestinations.MEDIA,
                        onClick = { navigate(AppDestinations.MEDIA) },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Media") },
                        label = { Text("Media") }
                    )
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                when (currentDestination) {
                    AppDestinations.HOME -> HomeScreen(
                        info = churchInfo,
                        events = events,
                        innerPadding = innerPadding,
                        onOpenChat = { navigate(AppDestinations.CHAT) },
                        onOpenMedia = { navigate(AppDestinations.MEDIA) },
                        onOpenEvents = { navigate(AppDestinations.EVENTS) }
                    )
                    AppDestinations.EVENTS -> EventsScreen(events, innerPadding)
                    AppDestinations.MEDIA -> MediaScreen(mediaItems, churchInfo.liveStream, innerPadding)
                    AppDestinations.CHAT -> ChatScreen(innerPadding)
                    AppDestinations.ACCOUNT -> ChatScreen(innerPadding)
                }
            }
        }
    }
}

enum class AppDestinations(val label: String) {
    HOME("Home"),
    EVENTS("Events"),
    MEDIA("Media"),
    CHAT("Chat"),
    ACCOUNT("Account")
}
