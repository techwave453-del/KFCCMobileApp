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
import com.example.helloworld.admin.AdminRoles

private val assignablePermissions = listOf(
    AdminPermissions.USERS_VIEW,
    AdminPermissions.USERS_CREATE,
    AdminPermissions.USERS_EDIT,
    AdminPermissions.USERS_DISABLE,
    AdminPermissions.USERS_DELETE,
    AdminPermissions.USERS_PERMISSIONS,
    AdminPermissions.AUDIT_VIEW,
    AdminPermissions.IDENTITY_VIEW,
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

private val rolePresets = mapOf(
    AdminRoles.CONTENT_EDITOR to listOf(AdminPermissions.SITE_EDIT, AdminPermissions.HOMEPAGE_EDIT, AdminPermissions.ABOUT_EDIT, AdminPermissions.SERVICES_EDIT, AdminPermissions.LINKS_EDIT, AdminPermissions.CLASSES_EDIT, AdminPermissions.GALLERY_EDIT, AdminPermissions.THEME_EDIT),
    AdminRoles.MEDIA_MANAGER to listOf(AdminPermissions.MEDIA_VIEW, AdminPermissions.MEDIA_UPLOAD, AdminPermissions.MEDIA_EDIT, AdminPermissions.MEDIA_DELETE),
    AdminRoles.LIVE_MANAGER to listOf(AdminPermissions.LIVE_VIEW, AdminPermissions.LIVE_MANAGE, AdminPermissions.COMMENTS_VIEW, AdminPermissions.COMMENTS_MODERATE),
    AdminRoles.SYSTEM_ADMIN to listOf(AdminPermissions.USERS_VIEW, AdminPermissions.USERS_CREATE, AdminPermissions.USERS_EDIT, AdminPermissions.USERS_DISABLE, AdminPermissions.USERS_DELETE, AdminPermissions.USERS_PERMISSIONS, AdminPermissions.AUDIT_VIEW)
)

private fun roleLabel(role: String): String = when (role) {
    AdminRoles.CONTENT_EDITOR -> "Content Editor"
    AdminRoles.MEDIA_MANAGER -> "Media Manager"
    AdminRoles.LIVE_MANAGER -> "Live Manager"
    AdminRoles.SYSTEM_ADMIN -> "System Administrator"
    AdminRoles.SUPER_ADMIN -> "Super Admin"
    else -> "Custom Permissions"
}

@Composable
fun AdminUsersScreen(modifier: Modifier = Modifier, viewModel: AdminUsersViewModel, onBack: () -> Unit) {
    val users by viewModel.users.collectAsState()
    val requests by viewModel.requests.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()
    var approvalRequest by remember { mutableStateOf<AdminAccessRequest?>(null) }
    var permissionUser by remember { mutableStateOf<AdminManagedUser?>(null) }
    var roleUser by remember { mutableStateOf<AdminManagedUser?>(null) }
    var selectedRole by remember { mutableStateOf(AdminRoles.CONTENT_EDITOR) }
    var selectedPermissions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmUser by remember { mutableStateOf<AdminManagedUser?>(null) }
    var confirmAction by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { viewModel.refresh() }

    if (approvalRequest != null) {
        var role by remember(approvalRequest?.id) { mutableStateOf(AdminRoles.CONTENT_EDITOR) }
        var permissions by remember(approvalRequest?.id) { mutableStateOf<Set<String>>(emptySet()) }
        AlertDialog(
            onDismissRequest = { if (!loading) approvalRequest = null },
            title = { Text("Approve ${approvalRequest!!.username}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Choose a role preset. You can fine-tune its permissions below.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(AdminRoles.CONTENT_EDITOR, AdminRoles.MEDIA_MANAGER, AdminRoles.LIVE_MANAGER, AdminRoles.SYSTEM_ADMIN).forEach { value ->
                            if (role == value) Button(onClick = { role = value; permissions = rolePresets[value]?.toSet().orEmpty() }) { Text(roleLabel(value)) }
                            else OutlinedButton(onClick = { role = value; permissions = rolePresets[value]?.toSet().orEmpty() }) { Text(roleLabel(value)) }
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

    if (roleUser != null) {
        AlertDialog(
            onDismissRequest = { if (!loading) roleUser = null },
            title = { Text("Role · " + roleUser!!.username) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose the administrator role. Permissions are kept as currently assigned.")
                    listOf(AdminRoles.CONTENT_EDITOR, AdminRoles.MEDIA_MANAGER, AdminRoles.LIVE_MANAGER, AdminRoles.SYSTEM_ADMIN).forEach { value ->
                        if (selectedRole == value) Button(onClick = { selectedRole = value }, modifier = Modifier.fillMaxWidth()) { Text(roleLabel(value)) }
                        else OutlinedButton(onClick = { selectedRole = value }, modifier = Modifier.fillMaxWidth()) { Text(roleLabel(value)) }
                    }
                }
            },
            confirmButton = { Button(onClick = { viewModel.setRole(roleUser!!, selectedRole); roleUser = null }, enabled = !loading && selectedRole != roleUser!!.role) { Text("Save role") } },
            dismissButton = { OutlinedButton(onClick = { roleUser = null }, enabled = !loading) { Text("Cancel") } }
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

    if (confirmUser != null && confirmAction != null) {
        val deleting = confirmAction == "delete"
        val enabling = confirmAction == "enable"
        AlertDialog(
            onDismissRequest = { if (!loading) { confirmUser = null; confirmAction = null } },
            title = { Text(if (deleting) "Delete administrator?" else if (enabling) "Enable administrator?" else "Disable administrator?") },
            text = {
                Text(
                    if (deleting) "This permanently removes " + confirmUser!!.username + " and their assigned permissions."
                    else if (enabling) "Allow " + confirmUser!!.username + " to sign in again?"
                    else "Prevent " + confirmUser!!.username + " from signing in until the account is enabled again."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val user = confirmUser!!
                        if (deleting) viewModel.delete(user) else viewModel.setStatus(user, enabling)
                        confirmUser = null
                        confirmAction = null
                    },
                    enabled = !loading
                ) { Text(if (deleting) "Delete" else if (enabling) "Enable" else "Disable") }
            },
            dismissButton = { OutlinedButton(onClick = { confirmUser = null; confirmAction = null }, enabled = !loading) { Text("Cancel") } }
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
                                    OutlinedButton(onClick = { selectedRole = when (user.role) { AdminRoles.MEDIA_MANAGER -> AdminRoles.MEDIA_MANAGER; AdminRoles.LIVE_MANAGER -> AdminRoles.LIVE_MANAGER; AdminRoles.SYSTEM_ADMIN -> AdminRoles.SYSTEM_ADMIN; else -> AdminRoles.CONTENT_EDITOR }; roleUser = user }, enabled = !loading) { Text("Role") }
                                    OutlinedButton(onClick = { selectedPermissions = user.permissions.toSet(); permissionUser = user }, enabled = !loading) { Text("Permissions") }
                                    OutlinedButton(onClick = { confirmUser = user; confirmAction = if (user.is_active) "disable" else "enable" }, enabled = !loading) { Text(if (user.is_active) "Disable" else "Enable") }
                                    OutlinedButton(onClick = { confirmUser = user; confirmAction = "delete" }, enabled = !loading) { Text("Delete") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
