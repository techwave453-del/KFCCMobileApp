package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.ui.PreferencesViewModel

@Composable
fun PreferencesScreen(
    innerPadding: PaddingValues,
    viewModel: PreferencesViewModel = viewModel()
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isAudioAutoplay by viewModel.isAudioAutoplay.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Text("App Preferences", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            
            PreferenceToggle(
                title = "Dark Mode",
                subtitle = "Enable a darker theme for the app",
                checked = isDarkMode,
                onCheckedChange = { viewModel.toggleDarkMode() }
            )
            
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            
            PreferenceToggle(
                title = "Audio Auto-play",
                subtitle = "Automatically start sermons when opened",
                checked = isAudioAutoplay,
                onCheckedChange = { viewModel.toggleAudioAutoplay() }
            )
        }
    }
}

@Composable
private fun PreferenceToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
