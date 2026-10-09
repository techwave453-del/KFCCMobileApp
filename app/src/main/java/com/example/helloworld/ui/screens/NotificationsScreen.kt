package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.data.AppNotification
import com.example.helloworld.ui.NotificationViewModel
import com.example.helloworld.ui.components.CopyableErrorMessage

@Composable
fun NotificationsScreen(
    innerPadding: PaddingValues,
    canViewNotifications: Boolean = false,
    onOpenBibleReference: (String, String?) -> Unit = { _, _ -> },
    viewModel: NotificationViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    // The notification center follows the same order as the Android notification tray:
    // Today's Scripture first, approved church/admin notifications next, then unread
    // chat messages. Read chat messages are not repeated here.
    val orderedNotifications = notifications
        .filterNot { notification ->
            notification.type.equals("chat_message", true) && notification.readAt != null
        }
        .sortedWith(
            compareBy<AppNotification> {
                when {
                    it.type.equals("daily_scripture", true) -> 0
                    it.type.equals("chat_message", true) -> 2
                    else -> 1
                }
            }.thenBy { it.readAt != null }
             .thenByDescending { it.createdAt }
        )
    val adminNotifications = orderedNotifications
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    val notificationsEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        if (!canViewNotifications) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Sign in to view notifications",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Church notifications sent to your account will appear here after you sign in.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (isLoading && adminNotifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null && adminNotifications.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CopyableErrorMessage(
                    message = error!!,
                    technicalDetails = error,
                    onRetry = viewModel::refresh
                )
            }
        } else if (adminNotifications.isEmpty()) {
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
                    val unreadCount = orderedNotifications.count { it.readAt == null }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Latest Updates",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (unreadCount > 0) {
                            AssistChip(
                                onClick = {},
                                label = { Text("$unreadCount unread") },
                                leadingIcon = { Icon(Icons.Default.MarkEmailUnread, contentDescription = null) }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (unreadCount > 0) "Today's Scripture, church updates and unread messages are shown here."
                        else "Today's Scripture and the latest approved church updates are shown here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(
                    orderedNotifications,
                    key = { it.id }
                ) { notification ->
                    NotificationCard(
                        notification = notification,
                        onViewed = { viewModel.markAsRead(notification.id) },
                        onOpenBibleReference = onOpenBibleReference
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    onViewed: () -> Unit,
    onOpenBibleReference: (String) -> Unit
) {
    var showDetails by remember(notification.id) { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        onClick = {
            onViewed()
            if (notification.type.equals("daily_scripture", true)) {
                val sections = notification.message.split("\n\n")
                val reference = sections.firstOrNull()?.trim().orEmpty()
                val explanation = sections.getOrNull(2)?.trim()
                if (reference.isNotBlank()) onOpenBibleReference(reference, explanation)
            } else {
                showDetails = true
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.readAt != null) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = when (notification.type) {
                    "welcome" -> Icons.Default.Celebration
                    "admin" -> Icons.Default.Campaign
                    "chat" -> Icons.AutoMirrored.Filled.Chat
                    "daily_scripture" -> Icons.Default.MenuBook
                    else -> Icons.Default.Notifications
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!notification.senderAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = notification.senderAvatarUrl,
                            contentDescription = "Sender profile picture",
                            modifier = Modifier.size(38.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (notification.readAt == null) {
                        Text(
                            "NEW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (!notification.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = notification.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(190.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(notification.message, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    notification.createdAt.replace("T", " ").replace("Z", " UTC"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!notification.type.equals("daily_scripture", true)) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap to view details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showDetails) {
        Dialog(
            onDismissRequest = { showDetails = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            notification.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showDetails = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close notification")
                        }
                    }

                    if (!notification.imageUrl.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            AsyncImage(
                                model = notification.imageUrl,
                                contentDescription = notification.title,
                                modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(notification.message, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            notification.createdAt.replace("T", " ").replace("Z", " UTC"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDetails = false },
                            modifier = Modifier.weight(1f)
                        ) { Text("Close") }
                        Button(
                            onClick = {
                                val shareText = buildString {
                                    append(notification.title)
                                    append("\n\n")
                                    append(notification.message)
                                    notification.imageUrl?.takeIf { it.isNotBlank() }?.let {
                                        append("\n\nImage: ")
                                        append(it)
                                    }
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        },
                                        "Share notification"
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Share")
                        }
                    }
                }
            }
        }
    }
}
