package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.LocalCache

@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
    onOpenAppearance: () -> Unit = {},
    onOpenNotifications: () -> Unit = {}
) {
    var showCacheDialog by remember { mutableStateOf(false) }
    var cacheCleared by remember { mutableStateOf(false) }

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
            Text(
                "Manage your Kanisa app preferences and local app data.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            SettingsItem(
                icon = Icons.Default.Palette,
                title = "Appearance",
                subtitle = "Choose light, dark, or system theme",
                onClick = onOpenAppearance
            )

            SettingsItem(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                subtitle = "Manage Kanisa notification settings",
                onClick = onOpenNotifications
            )

            SettingsItem(
                icon = Icons.Default.Security,
                title = "Account & security",
                subtitle = "Manage your account from the profile and sign-in screens",
                onClick = { }
            )

            SettingsItem(
                icon = Icons.Default.Cached,
                title = "Clear local cache",
                subtitle = "Refresh saved church content on the next load",
                onClick = { showCacheDialog = true }
            )

            if (cacheCleared) {
                Text(
                    "Local cache cleared. Reload content when you return to Home.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }

    if (showCacheDialog) {
        AlertDialog(
            onDismissRequest = { showCacheDialog = false },
            title = { Text("Clear local cache?") },
            text = {
                Text(
                    "This removes cached church and media content from this device. " +
                        "Your account and Supabase data are not deleted."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        LocalCache.clearCachedContent()
                        cacheCleared = true
                        showCacheDialog = false
                    }
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showCacheDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
