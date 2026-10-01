package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.ui.PreferencesViewModel

@Composable
fun AppearanceScreen(
    innerPadding: PaddingValues,
    viewModel: PreferencesViewModel = viewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()

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
                "Appearance",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Choose how Kanisa looks on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            AppearanceOption(
                title = "System default",
                subtitle = "Follow your Android light or dark mode",
                icon = Icons.Default.BrightnessAuto,
                selected = themeMode == "system",
                onClick = { viewModel.setThemeMode("system") }
            )
            AppearanceOption(
                title = "Light",
                subtitle = "Use the light theme",
                icon = Icons.Default.LightMode,
                selected = themeMode == "light",
                onClick = { viewModel.setThemeMode("light") }
            )
            AppearanceOption(
                title = "Dark",
                subtitle = "Use the darker theme for lower light",
                icon = Icons.Default.DarkMode,
                selected = themeMode == "dark",
                onClick = { viewModel.setThemeMode("dark") }
            )
        }
    }
}

@Composable
private fun AppearanceOption(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}
