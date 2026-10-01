package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.ui.PreferencesViewModel

@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
    onOpenAppearance: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    viewModel: PreferencesViewModel = viewModel()
) {
    val isAudioAutoplay by viewModel.isAudioAutoplay.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Manage the way Kanisa behaves and keeps you connected.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(22.dp))

            SettingsAction(
                icon = Icons.Default.Palette,
                title = "Appearance",
                subtitle = "Theme and display preferences",
                onClick = onOpenAppearance
            )
            SettingsAction(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                subtitle = "Open your church notifications",
                onClick = onOpenNotifications
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            ListItem(
                leadingContent = { Icon(Icons.Default.PlayCircle, contentDescription = null) },
                headlineContent = { Text("Audio auto-play", fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text("Start sermon audio automatically when available") },
                trailingContent = {
                    Switch(
                        checked = isAudioAutoplay,
                        onCheckedChange = { viewModel.toggleAudioAutoplay() }
                    )
                }
            )
        }
    }
}

@Composable
private fun SettingsAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        ListItem(
            leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text(subtitle) }
        )
    }
}
