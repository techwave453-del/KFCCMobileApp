package com.example.helloworld.ui.screens

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.helloworld.admin.AdminUser
import com.example.helloworld.admin.AdminViewModel
import com.example.helloworld.data.ChatMessage
import com.example.helloworld.ui.ChatViewModel
import com.example.helloworld.ui.ChurchViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    innerPadding: PaddingValues,
    viewModel: ChatViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel(factory = AdminViewModel.Factory(LocalContext.current.applicationContext as Application)),
    onAdminLoginSuccess: () -> Unit = {}
) {
    val churchViewModel: ChurchViewModel = viewModel()
    val churchInfo by churchViewModel.churchInfo.collectAsState()
    val signedIn by viewModel.signedIn.collectAsState()
    val adminUser by adminViewModel.user.collectAsState()

    if (signedIn || adminUser != null) {
        CommunityChat(viewModel, adminUser, churchInfo.churchName, innerPadding)
    } else {
        UnifiedAuthScreen(
            churchInfo = churchInfo,
            chatViewModel = viewModel,
            adminViewModel = adminViewModel,
            innerPadding = innerPadding,
            onMemberSignedIn = viewModel::initChat,
            onAdminLoginSuccess = onAdminLoginSuccess
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityChat(
    viewModel: ChatViewModel, 
    adminUser: AdminUser?, 
    churchName: String,
    innerPadding: PaddingValues
) {
    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val sending by viewModel.sending.collectAsState()
    val error by viewModel.error.collectAsState()
    val roomId by viewModel.roomId.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val replyingTo by viewModel.replyingTo.collectAsState()
    
    var input by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showRoomPicker by remember { mutableStateOf(false) }
    var showCreateGroup by remember { mutableStateOf(false) }
    var showGroupBrowser by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()
    val currentRoom = rooms.find { it.id == roomId }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Chat Header & Search - Fixed to Top
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = innerPadding.calculateTopPadding()),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (currentRoom?.type == "group") Icons.Default.Group else Icons.AutoMirrored.Filled.Chat, 
                        null, 
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = currentRoom?.title ?: (if (adminUser != null) "Community · Administrator Access" else "Community Conversation"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showRoomPicker = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ExpandMore, "Switch Room")
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = viewModel::initChat, modifier = Modifier.size(24.dp)) { 
                        Icon(Icons.Default.Refresh, "Refresh", modifier = Modifier.size(18.dp)) 
                    }
                }
                
                Spacer(Modifier.height(10.dp))
                
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    placeholder = { Text("Search messages…", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                    trailingIcon = if (search.isNotEmpty()) {
                        { IconButton(onClick = { search = "" }) { Icon(Icons.Default.Clear, null, Modifier.size(18.dp)) } }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
            }
        }

        // Chat messages Area
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (loading && messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                }
            } else {
                val visibleMessages = messages.filter { 
                    search.isBlank() || it.message.contains(search, ignoreCase = true) 
                }
                
                if (visibleMessages.isEmpty()) {
                    EmptyState(isSearch = search.isNotBlank())
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val grouped = visibleMessages.groupBy { it.createdAt.substringBefore("T") }
                        grouped.forEach { (date, messagesInDate) ->
                            item { DateHeader(date) }
                            items(messagesInDate, key = { it.id }) { message ->
                                ChatBubble(
                                    message = message,
                                    own = message.senderId == viewModel.currentUserId(),
                                    repliedToMessage = messages.find { it.id == message.replyToId },
                                    onEdit = { 
                                        editingMessage = it
                                        input = it.message
                                    },
                                    onDelete = { viewModel.deleteMessage(it.id) },
                                    onReply = { viewModel.setReplyingTo(it) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Interaction Area - Flush with Keyboard
        val imeVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = if (imeVisible) 0.dp else innerPadding.calculateBottomPadding())
        ) {
            ChatInput(
                input = input,
                onInputChange = { if (it.length <= 1000) input = it },
                onSend = {
                    if (editingMessage != null) {
                        viewModel.editMessage(editingMessage!!.id, input)
                        editingMessage = null
                    } else {
                        viewModel.sendMessage(input)
                    }
                    input = ""
                },
                sending = sending,
                enabled = roomId != null,
                isEditing = editingMessage != null,
                replyingTo = replyingTo,
                onCancelEdit = {
                    editingMessage = null
                    input = ""
                },
                onCancelReply = { viewModel.setReplyingTo(null) }
            )
        }
    }
    
    if (showRoomPicker) {
        AlertDialog(
            onDismissRequest = { showRoomPicker = false },
            title = { Text("Switch Room") },
            text = {
                Column {
                    rooms.forEach { room ->
                        TextButton(
                            onClick = { 
                                viewModel.selectRoom(room.id)
                                showRoomPicker = false 
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(if (room.type == "group") Icons.Default.Group else Icons.AutoMirrored.Filled.Chat, null)
                            Spacer(Modifier.width(12.dp))
                            Text(room.title, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                            if (room.id == roomId) Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    TextButton(
                        onClick = {
                            showRoomPicker = false
                            viewModel.loadDiscoverableGroups()
                            showGroupBrowser = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Explore, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Find Groups", modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                    }
                    TextButton(
                        onClick = {
                            showRoomPicker = false
                            showCreateGroup = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Create New Group", modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showGroupBrowser) {
        val discoverableGroups by viewModel.discoverableGroups.collectAsState()
        val joinRequests by viewModel.joinRequests.collectAsState()
        AlertDialog(
            onDismissRequest = { showGroupBrowser = false },
            title = { Text("Church Groups") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (discoverableGroups.isEmpty()) {
                        Text("No groups are available yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        discoverableGroups.forEach { group ->
                            val joined = rooms.any { it.id == group.id }
                            val request = joinRequests[group.id]
                            Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Group, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(group.title, fontWeight = FontWeight.Bold)
                                        Text(
                                            if (joined) "You're a member"
                                            else if (group.joinMode == "approval") "Approval required"
                                            else "Open to church members",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (request?.status == "pending") {
                                            Text("Join request pending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    when {
                                        joined -> TextButton(onClick = { viewModel.selectRoom(group.id); showGroupBrowser = false }) { Text("Open") }
                                        request?.status == "pending" -> TextButton(onClick = {}, enabled = false) { Text("Pending") }
                                        group.joinMode == "approval" -> Button(onClick = { viewModel.requestGroupJoin(group.id) }) { Text("Request") }
                                        else -> Button(onClick = { viewModel.joinGroup(group.id) }) { Text("Join") }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showGroupBrowser = false }) { Text("Done") } }
        )
    }

    if (showCreateGroup) {
        AlertDialog(
            onDismissRequest = { showCreateGroup = false },
            title = { Text("New Group") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createGroup(newGroupName)
                        newGroupName = ""
                        showCreateGroup = false
                    },
                    enabled = newGroupName.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroup = false }) { Text("Cancel") }
            }
        )
    }

    error?.let { errorMessage ->
        val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("Chat Connection") },
            text = { Text(errorMessage) },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(errorMessage))
                    }) {
                        Text("Copy error")
                    }
                    TextButton(onClick = viewModel::clearError) {
                        Text("OK")
                    }
                }
            }
        )
    }
}

@Composable
private fun EmptyState(isSearch: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isSearch) Icons.Default.SearchOff else Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = if (isSearch) "No messages match your search" else "Welcome to the KFCC Community!",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isSearch) "Try searching for something else." else "Start the conversation by sending a message below.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun DateHeader(dateStr: String) {
    val formattedDate = remember(dateStr) {
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }.time
            val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(yesterday)
            
            when (dateStr) {
                today -> "Today"
                yesterdayStr -> "Yesterday"
                else -> SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(date!!)
            }
        } catch (_: Exception) { dateStr }
    }
    
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = formattedDate,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(
    message: ChatMessage, 
    own: Boolean,
    repliedToMessage: ChatMessage? = null,
    onEdit: (ChatMessage) -> Unit = {},
    onDelete: (ChatMessage) -> Unit = {},
    onReply: (ChatMessage) -> Unit = {}
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val date = remember(message.createdAt) {
        try { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(message.createdAt.substringBefore('.')) }
        catch (_: Exception) { null }
    }
    val profile = message.senderProfile
    val isAdmin = profile?.is_admin_visible == true
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (own) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!own) {
            // ... (Avatar code remains same)
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (profile?.avatar_url?.isNotBlank() == true) {
                        AsyncImage(
                            model = profile.avatar_url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else if (isAdmin) {
                        Icon(Icons.Default.Security, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(
                            text = profile?.username?.take(1)?.uppercase() ?: "?",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (own) Alignment.End else Alignment.Start) {
            if (!own) {
                // ... (Username code remains same)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile?.username ?: "Member",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isAdmin) {
                        Spacer(Modifier.width(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = profile?.admin_role?.replace("_", " ")?.uppercase() ?: "ADMIN",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Box {
                Surface(
                    color = if (own) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = 16.dp, 
                        topEnd = 16.dp, 
                        bottomStart = if (own) 16.dp else 4.dp, 
                        bottomEnd = if (own) 4.dp else 16.dp
                    ),
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .combinedClickable(
                            onLongClick = { showMenu = true },
                            onClick = {}
                        )
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        if (repliedToMessage != null) {
                            Surface(
                                color = (if (own) Color.White else MaterialTheme.colorScheme.primary).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(bottom = 6.dp).fillMaxWidth()
                            ) {
                                Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Box(Modifier.width(2.dp).fillMaxHeight().background(if (own) Color.White else MaterialTheme.colorScheme.primary))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = repliedToMessage.senderProfile?.username ?: "Member",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (own) Color.White else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = repliedToMessage.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            color = (if (own) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = message.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (own) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                        ) {
                            if (message.editedAt != null) {
                                Text(
                                    text = "Edited",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = (if (own) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.5f),
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            Text(
                                text = if (date != null) timeFormat.format(date) else "",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = (if (own) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Reply") },
                        onClick = { showMenu = false; onReply(message) },
                        leadingIcon = { Icon(Icons.Default.Reply, null) }
                    )
                    if (own) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { showMenu = false; onEdit(message) },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete(message) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInput(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    sending: Boolean,
    enabled: Boolean,
    isEditing: Boolean = false,
    replyingTo: ChatMessage? = null,
    onCancelEdit: () -> Unit = {},
    onCancelReply: () -> Unit = {}
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            if (isEditing) {
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)).padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Editing message", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = onCancelEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                    }
                }
            }
            if (replyingTo != null) {
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Reply, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Replying to ${replyingTo.senderProfile?.username ?: "Member"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = replyingTo.message,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                    }
                }
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (isEditing) "Edit message…" else "Write a message…", fontSize = 15.sp) },
                    maxLines = 5,
                    enabled = enabled,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
                Spacer(Modifier.width(12.dp))
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = if (input.trim().isNotEmpty() && !sending && enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    IconButton(
                        onClick = onSend,
                        enabled = !sending && input.trim().isNotEmpty() && enabled
                    ) {
                        if (sending) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(
                                if (isEditing) Icons.Default.Check else Icons.AutoMirrored.Filled.Send, 
                                "Send",
                                tint = if (input.trim().isNotEmpty() && enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
