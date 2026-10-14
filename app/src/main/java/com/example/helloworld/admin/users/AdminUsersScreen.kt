package com.example.helloworld.admin.users
import com.example.helloworld.admin.AdminErrorMessage

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
import com.example.helloworld.admin.AdminUser
import com.example.helloworld.admin.hasPermission

private val assignablePermissions = listOf(
    AdminPermissions.USERS_VIEW,
    AdminPermissions.USERS_CREATE,
    AdminPermissions.USERS_EDIT,
    AdminPermissions.USERS_DISABLE,
    AdminPermissions.USERS_DELETE,
    AdminPermissions.USERS_PERMISSIONS,
    AdminPermissions.AUDIT_VIEW,
    AdminPermissions.SITE_EDIT,
    AdminPermissions.SERVICES_EDIT,
    AdminPermissions.LINKS_EDIT,
    AdminPermissions.EVENTS_MANAGE,
    AdminPermissions.NOTIFICATIONS_SEND,
    AdminPermissions.APP_UPDATE_MANAGE,
    AdminPermissions.DAILY_SCRIPTURE_MANAGE,
    AdminPermissions.BIBLE_GAMES_MANAGE,
    AdminPermissions.MEDIA_VIEW,
    AdminPermissions.MEDIA_UPLOAD,
    AdminPermissions.MEDIA_EDIT,
    AdminPermissions.MEDIA_DELETE,
    AdminPermissions.LIVE_MANAGE
)

private fun permissionLabel(permission: String): String = when (permission) {
    AdminPermissions.USERS_VIEW -> "Users & Members — view lists"
    AdminPermissions.USERS_CREATE -> "Users & Members — approve/create administrators"
    AdminPermissions.USERS_EDIT -> "Users & Members — edit roles and member profiles"
    AdminPermissions.USERS_DISABLE -> "Users & Members — enable/disable administrators"
    AdminPermissions.USERS_DELETE -> "Users & Members — delete administrator/member records"
    AdminPermissions.USERS_PERMISSIONS -> "Users & Members — manage assigned permissions"
    AdminPermissions.AUDIT_VIEW -> "System Administration — view security overview"
    AdminPermissions.SITE_EDIT -> "Website Content — edit CMS pages and sections"
    AdminPermissions.SERVICES_EDIT -> "Services & Giving — edit worship services"
    AdminPermissions.LINKS_EDIT -> "Services & Giving — edit the giving link"
    AdminPermissions.EVENTS_MANAGE -> "Events Management — create and manage events"
    AdminPermissions.NOTIFICATIONS_SEND -> "Notifications — send church announcements"
    AdminPermissions.APP_UPDATE_MANAGE -> "App Updates — configure app update URL/version"
    AdminPermissions.DAILY_SCRIPTURE_MANAGE -> "Today's Scripture — manage scripture content"
    AdminPermissions.BIBLE_GAMES_MANAGE -> "Bible Games — manage games and quizzes"
    AdminPermissions.MEDIA_VIEW -> "Media Center — view the library"
    AdminPermissions.MEDIA_UPLOAD -> "Media Center — upload/add media URLs"
    AdminPermissions.MEDIA_EDIT -> "Media Center — edit media details and publishing"
    AdminPermissions.MEDIA_DELETE -> "Media Center — delete media items"
    AdminPermissions.LIVE_MANAGE -> "Live Streaming — manage stream status and settings"
    else -> permission
}

private val rolePresets = mapOf(
    AdminRoles.CONTENT_EDITOR to listOf(AdminPermissions.SITE_EDIT),
    AdminRoles.MEDIA_MANAGER to listOf(AdminPermissions.MEDIA_VIEW, AdminPermissions.MEDIA_UPLOAD, AdminPermissions.MEDIA_EDIT, AdminPermissions.MEDIA_DELETE),
    AdminRoles.LIVE_MANAGER to listOf(AdminPermissions.LIVE_MANAGE),
    AdminRoles.SYSTEM_ADMIN to emptyList()
)

private fun roleLabel(role: String): String = when (role) {
    AdminRoles.CONTENT_EDITOR -> "Content Editor"
    AdminRoles.MEDIA_MANAGER -> "Media Manager"
    AdminRoles.LIVE_MANAGER -> "Live Manager"
    AdminRoles.SYSTEM_ADMIN -> "Custom Permissions"
    AdminRoles.SUPER_ADMIN -> "Super Admin"
    else -> "Custom Permissions"
}

@Composable
fun AdminUsersScreen(modifier: Modifier = Modifier, viewModel: AdminUsersViewModel, currentUser: AdminUser, onBack: () -> Unit) {
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
    val canCreateUsers = currentUser.hasPermission(AdminPermissions.USERS_CREATE)
    val canEditUsers = currentUser.hasPermission(AdminPermissions.USERS_EDIT)
    val canDisableUsers = currentUser.hasPermission(AdminPermissions.USERS_DISABLE)
    val canDeleteUsers = currentUser.hasPermission(AdminPermissions.USERS_DELETE)
    val canManagePermissions = currentUser.hasPermission(AdminPermissions.USERS_PERMISSIONS)
    val grantablePermissions = assignablePermissions.filter { permission ->
        currentUser.hasPermission(permission) && when (permission) {
            AdminPermissions.USERS_CREATE,
            AdminPermissions.USERS_EDIT,
            AdminPermissions.USERS_DISABLE,
            AdminPermissions.USERS_DELETE,
            AdminPermissions.USERS_PERMISSIONS -> currentUser.hasPermission(AdminPermissions.USERS_VIEW)
            AdminPermissions.MEDIA_UPLOAD,
            AdminPermissions.MEDIA_EDIT,
            AdminPermissions.MEDIA_DELETE -> currentUser.hasPermission(AdminPermissions.MEDIA_VIEW)
            else -> true
        }
    }
    LaunchedEffect(Unit) { viewModel.refresh() }

    if (approvalRequest != null) {
        var role by remember(approvalRequest?.id) { mutableStateOf(AdminRoles.CONTENT_EDITOR) }
        var permissions by remember(approvalRequest?.id) { mutableStateOf(rolePresets[AdminRoles.CONTENT_EDITOR]?.filter { currentUser.hasPermission(it) }?.toSet().orEmpty()) }
        AlertDialog(
            onDismissRequest = { if (!loading) approvalRequest = null },
            title = { Text("Approve ${approvalRequest!!.username}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Choose a role preset. You can fine-tune its permissions below.")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(AdminRoles.CONTENT_EDITOR, AdminRoles.MEDIA_MANAGER, AdminRoles.LIVE_MANAGER, AdminRoles.SYSTEM_ADMIN).forEach { value ->
                            if (role == value) Button(onClick = { role = value; permissions = rolePresets[value]?.filter { currentUser.hasPermission(it) }?.toSet().orEmpty() }, modifier = Modifier.fillMaxWidth()) { Text(roleLabel(value)) }
                            else OutlinedButton(onClick = { role = value; permissions = rolePresets[value]?.filter { currentUser.hasPermission(it) }?.toSet().orEmpty() }, modifier = Modifier.fillMaxWidth()) { Text(roleLabel(value)) }
                        }
                    }
                    grantablePermissions.forEach { permission ->
                        OutlinedButton(onClick = { permissions = if (permissions.contains(permission)) permissions - permission else permissions + permission }, modifier = Modifier.fillMaxWidth()) {
                            Text((if (permissions.contains(permission)) "✓ " else "") + permissionLabel(permission) + "\n" + permission)
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
                    Text("Choosing a role applies its standard permissions. You can customize permissions separately afterward.")
                    listOf(AdminRoles.CONTENT_EDITOR, AdminRoles.MEDIA_MANAGER, AdminRoles.LIVE_MANAGER, AdminRoles.SYSTEM_ADMIN).forEach { value ->
                        if (value != AdminRoles.SYSTEM_ADMIN || canManagePermissions) {
                            if (selectedRole == value) Button(onClick = { selectedRole = value }, modifier = Modifier.fillMaxWidth()) { Text(roleLabel(value)) }
                            else OutlinedButton(onClick = { selectedRole = value }, modifier = Modifier.fillMaxWidth()) { Text(roleLabel(value)) }
                        }
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
                    grantablePermissions.forEach { permission ->
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
            error?.let { message -> AdminErrorMessage(message = message, modifier = Modifier.padding(vertical = 8.dp)) }
            if (message != null) Text(message!!, color = MaterialTheme.colorScheme.primary)
            Text("Pending approvals", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(6.dp))
            if (requests.isEmpty()) Text("No pending approval requests.", style = MaterialTheme.typography.bodySmall)
            requests.filter { it.status == "pending" }.forEach { request ->
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(request.username, style = MaterialTheme.typography.titleMedium)
                        Text("Expires ${request.expires_at ?: "unknown"}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        if (canCreateUsers) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { approvalRequest = request }, enabled = !loading) { Text("Approve") }
                                OutlinedButton(onClick = { viewModel.reject(request) }, enabled = !loading) { Text("Decline") }
                            }
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
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (canEditUsers) {
                                        OutlinedButton(onClick = { selectedRole = when (user.role) { AdminRoles.MEDIA_MANAGER -> AdminRoles.MEDIA_MANAGER; AdminRoles.LIVE_MANAGER -> AdminRoles.LIVE_MANAGER; AdminRoles.SYSTEM_ADMIN -> AdminRoles.SYSTEM_ADMIN; else -> AdminRoles.CONTENT_EDITOR }; roleUser = user }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Role") }
                                    }
                                    if (canManagePermissions) {
                                        OutlinedButton(onClick = { selectedPermissions = user.permissions.filter { it in grantablePermissions }.toSet(); permissionUser = user }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Permissions") }
                                    }
                                    if (canDisableUsers) {
                                        OutlinedButton(onClick = { confirmUser = user; confirmAction = if (user.is_active) "disable" else "enable" }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text(if (user.is_active) "Disable" else "Enable") }
                                    }
                                    if (canDeleteUsers) {
                                        OutlinedButton(onClick = { confirmUser = user; confirmAction = "delete" }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Delete") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
