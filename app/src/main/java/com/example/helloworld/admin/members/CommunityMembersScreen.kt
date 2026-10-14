package com.example.helloworld.admin.members

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.helloworld.admin.AdminErrorMessage
import com.example.helloworld.admin.AdminUser
import com.example.helloworld.admin.AdminPermissions
import com.example.helloworld.admin.hasPermission

@Composable
fun CommunityMembersScreen(
    modifier: Modifier = Modifier,
    viewModel: CommunityMemberViewModel = viewModel(),
    currentUser: AdminUser
) {
    val members by viewModel.members.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var editingMember by remember { mutableStateOf<CommunityMember?>(null) }
    var roleMember by remember { mutableStateOf<CommunityMember?>(null) }
    var deletingMember by remember { mutableStateOf<CommunityMember?>(null) }

    val assignableRoles = listOf("member", "moderator", "leader", "content_editor", "media_manager", "live_manager", "custom")

    val roleDetails = mapOf(
        "member" to ("Standard Member" to "Standard community access. No administrator dashboard access."),
        "moderator" to ("Community Moderator" to "Community moderation role only; no administrator dashboard access."),
        "leader" to ("Ministry Leader" to "Community leadership role only; no administrator dashboard access."),
        "content_editor" to ("Content Editor" to "Edit approved website content and theme settings."),
        "media_manager" to ("Media Manager" to "Upload, edit, and delete media library items."),
        "live_manager" to ("Live Manager" to "Manage live-stream features and moderate comments."),
        "custom" to ("Custom Administrator" to "Choose exact module access and capabilities below. Only selected permissions will be granted.")
    )

    // These are the permissions the app currently understands. Church Identity is
    // intentionally excluded because it remains Super Admin-only.
    val customPermissionGroups = listOf(
        "Website Content" to listOf(
            "site.edit" to "Edit CMS pages and sections (full website-content access)"
        ),
        "Services & Giving" to listOf(
            "content.services.edit" to "View and edit worship services and service times",
            "content.links.edit" to "View and edit the online giving link"
        ),
        "Media Center" to listOf(
            "media.view" to "Open and view the media library",
            "media.upload" to "Upload new media and add media URLs",
            "media.edit" to "Edit media details and publication settings",
            "media.delete" to "Delete media items"
        ),
        "Live Streaming" to listOf(
            "live.edit" to "View and change stream status, title, URL and description"
        ),
        "Events & Outreach" to listOf(
            "events.manage" to "Create and manage church events",
            "notifications.send" to "Create and send church notifications",
            "daily_scripture.manage" to "Manage Today's Scripture themes and content",
            "bible_games.manage" to "Manage Bible games and quiz content",
            "app_update.manage" to "Configure Android app update details and APK URL"
        ),
        "Users & Security" to listOf(
            "users.view" to "Open member and administrator lists",
            "users.create" to "Approve or create administrator accounts",
            "users.edit" to "Edit administrator roles and member profiles",
            "users.disable" to "Enable or disable administrator accounts",
            "users.delete" to "Delete administrator accounts or community member profiles",
            "users.permissions" to "Assign administrator permissions within your own authority",
            "audit.view" to "Open the System Administration overview and view assigned permissions"
        )
    )
    val assignableCustomPermissions = remember(customPermissionGroups) {
        customPermissionGroups.flatMap { it.second }.map { it.first }.toSet()
    }

    val filteredMembers = remember(searchQuery, members) {
        if (searchQuery.isBlank()) members
        else members.filter { 
            it.safeUsername.contains(searchQuery, true) || 
            it.safeDisplayName.contains(searchQuery, true) ||
            it.safeBio.contains(searchQuery, true) ||
            it.email.orEmpty().contains(searchQuery, true)
        }
    }

    if (editingMember != null) {
        var username by remember(editingMember) { mutableStateOf(editingMember!!.username.orEmpty()) }
        var displayName by remember(editingMember) { mutableStateOf(editingMember!!.displayName.orEmpty()) }
        var bio by remember(editingMember) { mutableStateOf(editingMember!!.bio.orEmpty()) }

        AlertDialog(
            onDismissRequest = { if (!loading) editingMember = null },
            title = { Text("Edit Member Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio / About") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateMember(editingMember!!.userId, username, displayName, bio) { success ->
                            if (success) editingMember = null
                        }
                    },
                    enabled = !loading && username.isNotBlank()
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingMember = null }, enabled = !loading) {
                    Text("Cancel")
                }
            }
        )
    }

    if (roleMember != null) {
        var selectedRole by remember(roleMember) {
            mutableStateOf(
                roleMember!!.safeRole.takeIf { it in assignableRoles }
                    ?: if (roleMember!!.safeRole == "admin") "content_editor" else "member"
            )
        }

        var selectedCustomPermissions by remember(roleMember) {
            mutableStateOf(roleMember!!.safePermissions.filter { it in assignableCustomPermissions }.toSet())
        }
        fun updateCustomPermission(permission: String, checked: Boolean) {
            selectedCustomPermissions = when {
                permission == "users.view" && !checked ->
                    selectedCustomPermissions.filterNot { it.startsWith("users.") }.toSet()
                permission.startsWith("users.") && permission != "users.view" && checked ->
                    selectedCustomPermissions + permission + "users.view"
                permission == "media.view" && !checked ->
                    selectedCustomPermissions.filterNot { it.startsWith("media.") }.toSet()
                permission.startsWith("media.") && permission != "media.view" && checked ->
                    selectedCustomPermissions + permission + "media.view"
                checked -> selectedCustomPermissions + permission
                else -> selectedCustomPermissions - permission
            }
        }

        AlertDialog(
            onDismissRequest = { if (!loading) roleMember = null },
            title = { Text("Assign Member Role") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Choose a community role or administrator role. Preset roles receive their standard permissions; Custom uses only the checked permissions below.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))

                    assignableRoles.forEach { role ->
                        val isSelected = selectedRole == role
                        val (title, description) = roleDetails[role] ?: (role.uppercase() to "")
                        Surface(
                            onClick = { selectedRole = role },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = isSelected, onClick = { selectedRole = role })
                                    Spacer(Modifier.width(8.dp))
                                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 32.dp))
                            }
                        }
                    }

                    if (selectedRole == "custom") {
                        HorizontalDivider()
                        Text("Custom permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Select only the capabilities this person needs. Church Identity and Super Admin access cannot be granted here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        customPermissionGroups.forEach { (groupTitle, options) ->
                            Text(groupTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            options.forEach { (permission, label) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = permission in selectedCustomPermissions,
                                        onCheckedChange = { checked ->
                                            updateCustomPermission(permission, checked)
                                        }
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(label, style = MaterialTheme.typography.bodyMedium)
                                        Text(permission, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val permissionsToSave = if (selectedRole == "custom") {
                            selectedCustomPermissions.toList().sorted()
                        } else {
                            roleMember!!.safePermissions
                        }
                        viewModel.updateRole(roleMember!!.userId, roleMember!!.safeUsername, selectedRole, permissionsToSave) { success ->
                            if (success) roleMember = null
                        }
                    },
                    enabled = !loading
                ) {
                    Text("Save Role")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { roleMember = null }, enabled = !loading) {
                    Text("Cancel")
                }
            }
        )
    }

    if (deletingMember != null) {
        AlertDialog(
            onDismissRequest = { if (!loading) deletingMember = null },
            title = { Text("Delete member account?") },
            text = { Text("Are you sure you want to delete ${deletingMember!!.safeDisplayName}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMember(deletingMember!!.userId)
                        deletingMember = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !loading
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingMember = null }, enabled = !loading) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Community Members", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Manage registered app members, roles & permissions", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = viewModel::refresh, enabled = !loading) {
                Text("Refresh")
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider()
        Spacer(Modifier.height(14.dp))

        error?.let {
            AdminErrorMessage(message = it, modifier = Modifier.padding(bottom = 8.dp))
        }
        message?.let {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(it, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            placeholder = { Text("Search members by name or username...") },
            leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null, Modifier.size(18.dp)) } }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(14.dp))

        Text("Registered Members (${filteredMembers.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        if (loading && members.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredMembers.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No community members found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredMembers, key = { it.userId }) { member ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!member.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = member.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.size(50.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.safeDisplayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (member.username != null) {
                                    Text(
                                        text = "@${member.safeUsername} • Role: ${member.safeRole.replace('_', ' ').uppercase()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (!member.email.isNullOrBlank()) {
                                    Text(member.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (member.safePermissions.isNotEmpty()) {
                                    Text(
                                        text = "Permissions: ${member.safePermissions.joinToString(", ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (currentUser.role == "super_admin") {
                                    OutlinedButton(
                                        onClick = { roleMember = member },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Assign Role", fontSize = 11.sp)
                                    }
                                }
                                if (currentUser.hasPermission(AdminPermissions.USERS_EDIT)) {
                                    OutlinedButton(
                                        onClick = { editingMember = member },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Edit", fontSize = 11.sp)
                                    }
                                }
                                if (currentUser.hasPermission(AdminPermissions.USERS_DELETE)) {
                                    OutlinedButton(
                                        onClick = { deletingMember = member },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Delete", fontSize = 11.sp)
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
