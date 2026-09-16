package com.example.helloworld

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.helloworld.ui.ChurchViewModel
import com.example.helloworld.ui.screens.*
import com.example.helloworld.ui.theme.KFCCTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KFCCTheme { KFCCApp() } }
    }
}

@Composable
fun KFCCApp(viewModel: ChurchViewModel = viewModel()) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val churchInfo by viewModel.churchInfo.collectAsState()
    val mediaItems by viewModel.mediaItems.collectAsState()
    val events by viewModel.events.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.fillMaxWidth()
            ) {
                NavigationBarItem(
                    selected = currentDestination == AppDestinations.HOME,
                    onClick = { currentDestination = AppDestinations.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = currentDestination == AppDestinations.EVENTS,
                    onClick = { currentDestination = AppDestinations.EVENTS },
                    icon = { Icon(Icons.Default.Event, contentDescription = "Events") },
                    label = { Text("Events") }
                )
                NavigationBarItem(
                    selected = currentDestination == AppDestinations.MEDIA,
                    onClick = { currentDestination = AppDestinations.MEDIA },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Media") },
                    label = { Text("Media") }
                )
                NavigationBarItem(
                    selected = currentDestination == AppDestinations.CHAT,
                    onClick = { currentDestination = AppDestinations.CHAT },
                    icon = {
                        BadgedBox(badge = { if (currentDestination != AppDestinations.CHAT) Badge() }) {
                            Icon(Icons.Default.Chat, contentDescription = "Chat")
                        }
                    },
                    label = { Text("Chat") }
                )
            }
        }
    ) { innerPadding ->
        Surface(color = MaterialTheme.colorScheme.background) {
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(
                    info = churchInfo,
                    events = events,
                    innerPadding = innerPadding,
                    onOpenChat = { currentDestination = AppDestinations.CHAT },
                    onOpenMedia = { currentDestination = AppDestinations.MEDIA },
                    onOpenEvents = { currentDestination = AppDestinations.EVENTS }
                )
                AppDestinations.EVENTS -> EventsScreen(events, innerPadding)
                AppDestinations.MEDIA -> MediaScreen(mediaItems, churchInfo.liveStream, innerPadding)
                AppDestinations.CHAT -> ChatScreen(innerPadding)
            }
        }
    }
}

enum class AppDestinations(val label: String) {
    HOME("Home"),
    EVENTS("Events"),
    MEDIA("Media"),
    CHAT("Chat")
}
