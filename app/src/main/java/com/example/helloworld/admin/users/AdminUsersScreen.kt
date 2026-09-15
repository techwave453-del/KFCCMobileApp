package com.example.helloworld.admin.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdminUsersScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminUsersViewModel,
    onBack: () -> Unit
) {
    val users by viewModel.users.collectAsState()
    val requests by viewModel.requests.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh() }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Users & Permissions", style = MaterialTheme.typography.headlineSmall)
                    Text("Administrator accounts, roles and approval requests", style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = viewModel::refresh, enabled = !loading) { Icon(Icons.Default.Refresh, "Refresh") }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("Pending / recent approvals", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (requests.isEmpty()) Text("No pending approval requests.", style = MaterialTheme.typography.bodySmall)
            requests.forEach { request ->
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(request.username, style = MaterialTheme.typography.titleMedium)
                        Text("${request.status} · expires ${request.expires_at ?: "unknown"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Administrators", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (loading && users.isEmpty()) {
                CircularProgressIndicator()
            } else if (error != null && users.isEmpty()) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(users, key = { it.id }) { user ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Text(user.username, style = MaterialTheme.typography.titleMedium)
                                Text(user.role, style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (user.is_active) "Active" else "Disabled",
                                    color = if (user.is_active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                                if (user.permissions.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text("${user.permissions.size} assigned permissions", style = MaterialTheme.typography.bodySmall)
                                } else if (user.role != "super_admin") {
                                    Spacer(Modifier.height(6.dp))
                                    Text("No additional permissions assigned", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
