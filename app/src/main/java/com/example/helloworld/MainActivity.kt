package com.example.helloworld

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.helloworld.R
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
    val adminViewModel: AdminViewModel = viewModel(factory = AdminViewModel.Factory(LocalContext.current.applicationContext as Application))

    LaunchedEffect(isLoading) { if (!isLoading) startupComplete = true }
    if (!startupComplete) { KfccLoadingScreen(churchName = churchInfo.churchName); return }

    NavigationSuiteScaffold(
        navigationSuiteColors = NavigationSuiteDefaults.colors(navigationBarContainerColor = MaterialTheme.colorScheme.surface),
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(icon = { Icon(it.icon, it.label) }, label = { Text(it.label) }, selected = it == currentDestination, onClick = { currentDestination = it })
            }
        }
    ) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(churchInfo, events, innerPadding)
                AppDestinations.EVENTS -> EventsScreen(events, innerPadding)
                AppDestinations.MEDIA -> MediaScreen(mediaItems, churchInfo.liveStream, innerPadding)
                AppDestinations.ADMIN -> AdminShell(adminViewModel, Modifier.padding(innerPadding))
            }
        }
    }
}

@Composable
private fun KfccLoadingScreen(churchName: String) {
    val displayName = churchName.ifBlank { "Kingdom Fellowship Christian Church" }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background))).systemBarsPadding(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 32.dp)) {
            Surface(modifier = Modifier.size(116.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                Image(painterResource(R.mipmap.ic_launcher), "KFCC", Modifier.fillMaxSize().clip(CircleShape), ContentScale.Crop)
            }
            Text(displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("Revealing Christ to Nations", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
        }
    }
}

enum class AppDestinations(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    EVENTS("Events", Icons.Default.Event),
    MEDIA("Media", Icons.Default.PlayArrow),
    ADMIN("Admin", Icons.Default.AdminPanelSettings)
}
