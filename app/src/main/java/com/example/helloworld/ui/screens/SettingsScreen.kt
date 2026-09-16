package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    Column(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        ListItem(headlineContent = { Text("Privacy") }, supportingContent = { Text("Manage app privacy and data controls.") })
        ListItem(headlineContent = { Text("Storage & cache") }, supportingContent = { Text("Manage locally cached church content.") })
        ListItem(headlineContent = { Text("Support") }, supportingContent = { Text("Get help with the KFCC mobile app.") })
        ListItem(headlineContent = { Text("Legal") }, supportingContent = { Text("Terms, privacy policy and other notices.") })
    }
}

@Composable
fun AboutScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    Column(Modifier.fillMaxSize().padding(innerPadding).padding(24.dp)) {
        Text("KFCC Mobile", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Version ${com.example.helloworld.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("Kingdom Fellowship Christian Church", style = MaterialTheme.typography.titleMedium)
        Text("Revealing Christ to Nations", style = MaterialTheme.typography.bodyMedium)
    }
}
