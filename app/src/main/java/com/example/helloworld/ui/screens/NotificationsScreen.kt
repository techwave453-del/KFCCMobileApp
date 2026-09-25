package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.data.AppNotification
import com.example.helloworld.ui.NotificationViewModel

@Composable
fun NotificationsScreen(
    innerPadding: PaddingValues,
    viewModel: NotificationViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    val notificationsEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    LaunchedEffect(notifications) {
        if (notifications.isNotEmpty() && notifications.any { it.readAt == null }) {
            viewModel.markAllAsRead()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        if (isLoading && notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null && notifications.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = viewModel::refresh) { Text("Retry") }
            }
        } else if (notifications.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(16.dp))
                Text("Notifications", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("You have no new notifications from KFCC.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    if (!notificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Enable phone notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Text("Allow KFCC to show church announcements in your Android notification drawer.", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                    Text("Enable Notifications")
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    Text(
                        "Latest Updates",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(notifications) { notification ->
                    NotificationCard(notification)
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: AppNotification) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.readAt != null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = when(notification.type) {
                    "welcome" -> Icons.Default.Celebration
                    "admin" -> Icons.Default.Campaign
                    "chat" -> Icons.AutoMirrored.Filled.Chat
                    else -> Icons.Default.Notifications
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(notification.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(notification.message, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    notification.createdAt.replace("T", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
