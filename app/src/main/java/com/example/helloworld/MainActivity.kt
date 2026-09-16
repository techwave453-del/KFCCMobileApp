package com.example.helloworld

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
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
        topBar = {
            TopAppBar(
                title = { Text(currentDestination.label) },
                actions = {
                    IconButton(onClick = { currentDestination = AppDestinations.NOTIFICATIONS }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(currentDestination == AppDestinations.HOME, { currentDestination = AppDestinations.HOME }, { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") })
                NavigationBarItem(currentDestination == AppDestinations.EVENTS, { currentDestination = AppDestinations.EVENTS }, { Icon(Icons.Default.Event, "Events") }, label = { Text("Events") })
                NavigationBarItem(currentDestination == AppDestinations.MEDIA, { currentDestination = AppDestinations.MEDIA }, { Icon(Icons.Default.PlayArrow, "Media") }, label = { Text("Media") })
                NavigationBarItem(currentDestination == AppDestinations.CHAT, { currentDestination = AppDestinations.CHAT }, { Icon(Icons.Default.Chat, "Chat") }, label = { Text("Chat") })
                NavigationBarItem(currentDestination == AppDestinations.PROFILE, { currentDestination = AppDestinations.PROFILE }, { Icon(Icons.Default.Person, "Profile") }, label = { Text("Profile") })
            }
        },
        floatingActionButton = {
            if (currentDestination != AppDestinations.CHAT && currentDestination != AppDestinations.NOTIFICATIONS) {
                ExtendedFloatingActionButton(
                    onClick = { currentDestination = AppDestinations.CHAT },
                    icon = { Icon(Icons.Default.Chat, "Open Chat") },
                    text = { Text("Chat") }
                )
            }
        }
    ) { innerPadding ->
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(churchInfo, events, innerPadding, { currentDestination = AppDestinations.CHAT }, { currentDestination = AppDestinations.MEDIA }, { currentDestination = AppDestinations.EVENTS })
                AppDestinations.EVENTS -> EventsScreen(events, innerPadding)
                AppDestinations.MEDIA -> MediaScreen(mediaItems, churchInfo.liveStream, innerPadding)
                AppDestinations.CHAT -> ChatScreen(innerPadding)
                AppDestinations.NOTIFICATIONS -> NotificationsScreen(innerPadding)
                AppDestinations.PROFILE -> ProfileScreen(innerPadding)
            }
        }
    }
}

enum class AppDestinations(val label: String) {
    HOME("Home"), EVENTS("Events"), MEDIA("Media"), CHAT("Chat"), NOTIFICATIONS("Notifications"), PROFILE("Profile")
}
