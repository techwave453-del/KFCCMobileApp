package com.example.helloworld

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.ui.ChurchViewModel
import com.example.helloworld.ui.screens.*
import com.example.helloworld.ui.theme.HelloWorldTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelloWorldTheme {
                HelloWorldApp()
            }
        }
    }
}

@Composable
fun HelloWorldApp(viewModel: ChurchViewModel = viewModel()) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val churchInfo by viewModel.churchInfo.collectAsState()
    val mediaItems by viewModel.mediaItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            imageVector = it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold { innerPadding ->
            Surface(color = MaterialTheme.colorScheme.background) {
                if (isLoading && churchInfo.churchName == "Kingdom Fellowship Christian Church") {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                when (currentDestination) {
                    AppDestinations.HOME -> HomeScreen(churchInfo, innerPadding)
                    AppDestinations.EVENTS -> EventsScreen(churchInfo, innerPadding)
                    AppDestinations.MEDIA -> MediaScreen(mediaItems, innerPadding)
                    AppDestinations.ADMIN -> {
                        if (currentUser == null) {
                            LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = { /* Stay on Admin, will show Dashboard */ },
                                innerPadding = innerPadding
                            )
                        } else {
                            var showUpload by remember { mutableStateOf(false) }
                            if (showUpload) {
                                MediaUploadScreen(
                                    viewModel = viewModel,
                                    onBack = { showUpload = false },
                                    innerPadding = innerPadding
                                )
                            } else {
                                AdminDashboard(
                                    viewModel = viewModel,
                                    onNavigateToUpload = { showUpload = true },
                                    onLogout = { /* Navigation state handled by currentUser check */ },
                                    innerPadding = innerPadding
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    EVENTS("Events", Icons.Default.Event),
    MEDIA("Media", Icons.Default.PlayArrow),
    ADMIN("Admin", Icons.Default.AdminPanelSettings),
}
