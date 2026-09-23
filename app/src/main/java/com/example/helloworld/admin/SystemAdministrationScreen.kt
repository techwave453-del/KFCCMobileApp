package com.example.helloworld.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SystemAdministrationScreen(user: AdminUser, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("System Administration", style = MaterialTheme.typography.headlineSmall)
        Text("Security and administrator session overview", style = MaterialTheme.typography.bodyMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current administrator", style = MaterialTheme.typography.titleMedium)
                Text("Username: ${user.username}")
                Text("Role: ${if (user.role == AdminRoles.SUPER_ADMIN) "Super Admin" else user.role}")
                Text("Status: ${if (user.is_active) "Active" else "Disabled"}")
                Text("Permissions: ${if (user.role == AdminRoles.SUPER_ADMIN) "All permissions (Super Admin)" else user.permissions.size}")
            }
        }
        Text("Assigned permissions", style = MaterialTheme.typography.titleMedium)
        if (user.role == AdminRoles.SUPER_ADMIN) Text("Super Admin has full administrator permissions.")
        else if (user.permissions.isEmpty()) Text("No administrator permissions are assigned.")
        else LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(user.permissions.sorted()) { permission ->
                Card(Modifier.fillMaxWidth()) { Text(permission, modifier = Modifier.padding(12.dp)) }
            }
        }
        Text("Security-sensitive changes are enforced by the server authorization layer. This screen does not expose raw database or JSON editing.", style = MaterialTheme.typography.bodySmall)
    }
}