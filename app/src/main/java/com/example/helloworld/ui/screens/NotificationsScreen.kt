package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.AppNotification
import com.example.helloworld.data.NotificationRepository

@Composable
fun NotificationsScreen(innerPadding: PaddingValues) {
    val repository = remember { NotificationRepository() }
    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        loading = true
        error = null
        repository.getNotifications()
            .onSuccess { notifications = it }
            .onFailure { error = it.message ?: "Unable to load notifications." }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Notifications", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            if (notifications.any { it.readAt == null }) {
                IconButton(
                    onClick = {
                        androidx.compose.runtime.LaunchedEffect(Unit) { }
                    },
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = "Mark all as read")
                }
            }
        }

        if (loading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }
        } else if (error != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { androidx.compose.runtime.LaunchedEffect(Unit) { } }) { Text("Retry") }
            }
        } else if (notifications.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null)
                Spacer(Modifier.height(12.dp))
                Text("You're all caught up", style = MaterialTheme.typography.titleMedium)
                Text("Church announcements and reminders will appear here.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationCard(
                        notification = notification,
                        onRead = {
                            if (notification.readAt == null) {
                                repository.markAsRead(notification.id).onSuccess {
                                    notifications = notifications.map {
                                        if (it.id == notification.id) it.copy(readAt = "read") else it
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    onRead: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onRead,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    notification.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (notification.readAt == null) {
                    Text("NEW", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(notification.message, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(notification.createdAt, style = MaterialTheme.typography.labelSmall)
        }
    }
}
