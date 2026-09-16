package com.example.helloworld

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.helloworld.admin.AdminShell
import com.example.helloworld.admin.AdminViewModel
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
    var startupComplete by rememberSaveable { mutableStateOf(false) }
    val churchInfo by viewModel.churchInfo.collectAsState()
    val mediaItems by viewModel.mediaItems.collectAsState()
    val events by viewModel.events.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val adminViewModel: AdminViewModel = viewModel(
        factory = AdminViewModel.Factory(LocalContext.current.applicationContext as Application)
    )

    LaunchedEffect(isLoading) {
        if (!isLoading) startupComplete = true
    }

    if (!startupComplete) {
        KfccLoadingScreen(churchName = churchInfo.churchName)
        return
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold { innerPadding ->
            Surface(color = MaterialTheme.colorScheme.background) {
                when (currentDestination) {
                    AppDestinations.HOME -> HomeScreen(churchInfo, events, innerPadding)
                    AppDestinations.EVENTS -> EventsScreen(events, innerPadding)
                    AppDestinations.MEDIA -> MediaScreen(mediaItems, churchInfo.liveStream, innerPadding)
                    AppDestinations.ADMIN -> AdminShell(adminViewModel, Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun KfccLoadingScreen(churchName: String) {
    val displayName = churchName.ifBlank { "Kingdom Fellowship Christian Church" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Do not load R.mipmap.ic_launcher with painterResource here.
            // Adaptive launcher icons are XML resources and cannot be decoded
            // by Compose's painterResource() as a Painter. Using a Material
            // ImageVector keeps the loading screen safe during startup.
            Surface(
                modifier = Modifier.size(112.dp),
                shape = CircleShape,
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "KFCC",
                        modifier = Modifier.size(58.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text(displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Preparing your church experience…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        }
    }
}

enum class AppDestinations(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    EVENTS("Events", Icons.Default.Event),
    MEDIA("Media", Icons.Default.PlayArrow),
    ADMIN("Admin", Icons.Default.AdminPanelSettings)
}
