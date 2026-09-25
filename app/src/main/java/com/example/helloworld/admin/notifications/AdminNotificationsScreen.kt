package com.example.helloworld.admin.notifications

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.data.AppNotification

@Composable
fun AdminNotificationsScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminNotificationsViewModel = viewModel(
        factory = AdminNotificationsViewModel.Factory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as Application
        )
    )
) {
    val sending by viewModel.sending.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val message by viewModel.message.collectAsState()
    val error by viewModel.error.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("general") }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Campaign, contentDescription = null)
                Column(Modifier.weight(1f)) {
                    Text("Notifications", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Send, manage and choose the notifications shown on app install and sign-in.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = viewModel::refresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }

        item {
            OutlinedTextField(
                title,
                { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                body,
                { body = it },
                label = { Text("Message") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                type,
                { type = it },
                label = { Text("Notification type") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.send(title, body, type) },
                    enabled = !sending && title.isNotBlank() && body.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (sending) CircularProgressIndicator(Modifier.size(18.dp))
                    else Text("Send Notification")
                }
                OutlinedButton(
                    onClick = { title = ""; body = ""; type = "general" },
                    enabled = !sending,
                    modifier = Modifier.weight(1f)
                ) { Text("Clear") }
            }
        }

        message?.let { text ->
            item { Text(text, color = MaterialTheme.colorScheme.primary) }
        }
        error?.let { text ->
            item { Text(text, color = MaterialTheme.colorScheme.error) }
        }

        item {
            HorizontalDivider()
            Text(
                "Notification History",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "The selected install and sign-in notifications are applied automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (loading && notifications.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        if (!loading && notifications.isEmpty()) {
            item { Text("No sent notifications yet.") }
        }

        items(notifications, key = { it.id }) { notification ->
            NotificationHistoryCard(
                notification = notification,
                onEnabledChanged = {
                    viewModel.update(notification.copy(isEnabled = it))
                },
                onInstallChanged = { viewModel.setInstallDefault(notification, it) },
                onSignInChanged = { viewModel.setSignInDefault(notification, it) },
                onDelete = { viewModel.delete(notification) }
            )
        }
    }
}

@Composable
private fun NotificationHistoryCard(
    notification: AppNotification,
    onEnabledChanged: (Boolean) -> Unit,
    onInstallChanged: (Boolean) -> Unit,
    onSignInChanged: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(notification.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        notification.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        notification.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete notification",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enabled")
                Switch(checked = notification.isEnabled, onCheckedChange = onEnabledChanged)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Show on install")
                Switch(checked = notification.showOnInstall, onCheckedChange = onInstallChanged)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Show on sign-in")
                Switch(checked = notification.showOnSignIn, onCheckedChange = onSignInChanged)
            }
        }
    }
}
