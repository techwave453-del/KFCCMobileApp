package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(innerPadding: PaddingValues) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            
            SettingsItem(
                title = "Update Profile",
                subtitle = "Change your username or email",
                onClick = { /* Navigate to edit profile */ }
            )
            
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            
            SettingsItem(
                title = "Security",
                subtitle = "Change your password",
                onClick = { /* Navigate to change password */ }
            )
            
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            
            SettingsItem(
                title = "Clear Cache",
                subtitle = "Reset local data to refresh church info",
                onClick = { /* Clear local cache */ }
            )
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
