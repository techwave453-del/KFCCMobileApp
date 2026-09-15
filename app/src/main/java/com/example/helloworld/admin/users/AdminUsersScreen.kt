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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.helloworld.admin.AdminPermissions

private val assignablePermissions = listOf(
    AdminPermissions.SITE_EDIT,
    AdminPermissions.HOMEPAGE_EDIT,
    AdminPermissions.ABOUT_EDIT,
    AdminPermissions.SERVICES_EDIT,
    AdminPermissions.LINKS_EDIT,
    AdminPermissions.CLASSES_EDIT,
    AdminPermissions.GALLERY_EDIT,
    AdminPermissions.THEME_EDIT,
    AdminPermissions.MEDIA_VIEW,
    AdminPermissions.MEDIA_UPLOAD,
    AdminPermissions.MEDIA_EDIT,
    AdminPermissions.MEDIA_DELETE,
    AdminPermissions.LIVE_VIEW,
    AdminPermissions.LIVE_MANAGE,
    AdminPermissions.COMMENTS_VIEW,
    AdminPermissions.COMMENTS_MODERATE
)

@Composable
fun AdminUsersScreen(modifier: Modifier = Modifier, viewModel: AdminUsersViewModel, onBack: () -> Unit) {
    val users by viewModel.users.collectAsState()
    val requests by viewModel.requests.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()
    var approvalRequest by remember { mutableStateOf<AdminAccessRequest?>(null) }
    var permissionUser by remember { mutableStateOf<AdminManagedUser?>(null) }
    var selectedPermissions by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(Unit) { viewModel.refresh() }

    if (approvalRequest != null) {
        var role by remember(approvalRequest?.id) { mutableStateOf("custom") }
        var permissions by remember(approvalRequest?.id) { mutableStateOf<Set<String>>(emptySet()) }
        AlertDialog(
            onDismissRequest = { if (!loading) approvalRequest = null },
            title = { Text("Approve ${approvalRequest!!.username}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Choose the administrator role and permissions.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("custom", "admin").forEach { value ->
                            if (role == value) Button(onClick = { role = value }) { Text(value) }
                            else OutlinedButton(onClick = { role = value }) { Text(value) }
                        }
                    }
                    assignablePermissions.forEach { permission ->
                        OutlinedButton(onClick = { permissions = if (permissions.contains(permission)) permissions - permission else permissions + permission }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (permissions.contains(permission)) "✓ $permission" else permission)
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { viewModel.approve(approvalRequest!!, role, permissions.toList()); approvalRequest = null }, enabled = !loading) { Text("Approve") } },
            dismissButton = { OutlinedButton(onClick = { approvalRequest = null }, enabled = !loading) { Text("Cancel") } }
        )
    }

    if (permissionUser != null) {
        AlertDialog(
            onDismissRequest = { if (!loading) permissionUser = null },
            title = { Text("Permissions · ${permissionUser!!.username}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    assignablePermissions.forEach { permission ->
                        OutlinedButton(onClick = { selectedPermissions = if (selectedPermissions.contains(permission)) selectedPermissions - permission else selectedPermissions + permission }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (selectedPermissions.contains(permission)) "✓ $permission" else permission)
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { viewModel.setPermissions(permissionUser!!, selectedPermissions.toList()); permissionUser = null }, enabled = !loading) { Text("Save permissions") } },
            dismissButton = { OutlinedButton(onClick = { permissionUser = null }, enabled = !loading) { Text("Cancel") } }
        )
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Users & Permissions", style = MaterialTheme.typography.headlineSmall)
                    Text("Administrator accounts, approvals, roles and permissions", style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedButton(onClick = viewModel::refresh, enabled = !loading) { Text("Refresh") }
            }
            Spacer(Modifier.height(12.dp)); HorizontalDivider(); Spacer(Modifier.height(12.dp))
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            if (message != null) Text(message!!, color = MaterialTheme.colorScheme.primary)
            Text("Pending approvals", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(6.dp))
            if (requests.isEmpty()) Text("No pending approval requests.", style = MaterialTheme.typography.bodySmall)
            requests.filter { it.status == "pending" }.forEach { request ->
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(request.username, style = MaterialTheme.typography.titleMedium)
                        Text("Expires ${request.expires_at ?: "unknown"}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { approvalRequest = request }, enabled = !loading) { Text("Approve") }
                            OutlinedButton(onClick = { viewModel.reject(request) }, enabled = !loading) { Text("Decline") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp)); Text("Administrators", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(6.dp))
            if (loading && users.isEmpty()) CircularProgressIndicator()
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(users, key = { it.id }) { user ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(user.username, style = MaterialTheme.typography.titleMedium)
                            Text(user.role, style = MaterialTheme.typography.labelMedium)
                            Text(if (user.is_active) "Active" else "Disabled", color = if (user.is_active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            Text("${user.permissions.size} assigned permissions", style = MaterialTheme.typography.bodySmall)
                            if (user.role != "super_admin") {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { selectedPermissions = user.permissions.toSet(); permissionUser = user }, enabled = !loading) { Text("Permissions") }
                                    OutlinedButton(onClick = { viewModel.setStatus(user, !user.is_active) }, enabled = !loading) { Text(if (user.is_active) "Disable" else "Enable") }
                                    OutlinedButton(onClick = { viewModel.delete(user) }, enabled = !loading) { Text("Delete") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
