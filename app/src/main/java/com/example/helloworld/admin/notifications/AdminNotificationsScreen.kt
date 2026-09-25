package com.example.helloworld.admin.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AdminNotificationsScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminNotificationsViewModel = viewModel(
        factory = AdminNotificationsViewModel.Factory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val sending by viewModel.sending.collectAsState()
    val message by viewModel.message.collectAsState()
    val error by viewModel.error.collectAsState()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("general") }

    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Campaign, contentDescription = null)
            Column {
                Text("Send Notification", style = MaterialTheme.typography.headlineSmall)
                Text("Send a church-wide notification to signed-in members.", style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(body, { body = it }, label = { Text("Message") }, minLines = 4, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(type, { type = it }, label = { Text("Notification type") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = { viewModel.send(title, body, type) { title = ""; body = ""; type = "general" } },
            enabled = !sending && title.isNotBlank() && body.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (sending) CircularProgressIndicator(Modifier.size(18.dp)) else Text("Send Notification")
        }
    }
}
