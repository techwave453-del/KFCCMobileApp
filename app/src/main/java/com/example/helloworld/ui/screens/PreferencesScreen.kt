package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PreferencesScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    var theme by rememberSaveable { mutableStateOf("System") }
    var autoplay by rememberSaveable { mutableStateOf(true) }
    var chatNotifications by rememberSaveable { mutableStateOf(true) }
    var churchNotifications by rememberSaveable { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
        Text("Preferences", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            listOf("System", "Light", "Dark").forEach { option ->
                FilterChip(selected = theme == option, onClick = { theme = option }, label = { Text(option) })
            }
        }
        HorizontalDivider()
        ListItem(headlineContent = { Text("Audio autoplay") }, trailingContent = { Switch(autoplay, { autoplay = it }) })
        ListItem(headlineContent = { Text("Chat notifications") }, trailingContent = { Switch(chatNotifications, { chatNotifications = it }) })
        ListItem(headlineContent = { Text("Church announcements") }, trailingContent = { Switch(churchNotifications, { churchNotifications = it }) })
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text("Account", style = MaterialTheme.typography.titleMedium)
        Text("Username, email and password controls are available from Profile.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
    }
}
