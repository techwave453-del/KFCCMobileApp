package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.AppPreferences

@Composable
fun PreferencesScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    preferences: AppPreferences,
    theme: String,
    onThemeChanged: (String) -> Unit,
) {
    var autoplay by remember(preferences) { mutableStateOf(preferences.audioAutoplay) }
    var chatNotifications by remember(preferences) { mutableStateOf(preferences.chatNotifications) }
    var churchNotifications by remember(preferences) { mutableStateOf(preferences.churchNotifications) }

    Column(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
        Text("Preferences", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            listOf("System", "Light", "Dark").forEach { option ->
                FilterChip(
                    selected = theme == option,
                    onClick = {
                        preferences.theme = option
                        onThemeChanged(option)
                    },
                    label = { Text(option) }
                )
            }
        }

        HorizontalDivider()
        Text("Audio", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
        ListItem(
            headlineContent = { Text("Audio autoplay") },
            supportingContent = { Text("Automatically start supported audio and video playback.") },
            trailingContent = {
                Switch(
                    checked = autoplay,
                    onCheckedChange = {
                        autoplay = it
                        preferences.audioAutoplay = it
                    }
                )
            }
        )

        HorizontalDivider()
        Text("Notifications", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
        ListItem(
            headlineContent = { Text("Chat notifications") },
            trailingContent = {
                Switch(
                    checked = chatNotifications,
                    onCheckedChange = {
                        chatNotifications = it
                        preferences.chatNotifications = it
                    }
                )
            }
        )
        ListItem(
            headlineContent = { Text("Church announcements") },
            trailingContent = {
                Switch(
                    checked = churchNotifications,
                    onCheckedChange = {
                        churchNotifications = it
                        preferences.churchNotifications = it
                    }
                )
            }
        )

        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text("Account", style = MaterialTheme.typography.titleMedium)
        Text(
            "Username, email and sign-out controls are available from Profile. Password changes will use the Supabase account security flow.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
