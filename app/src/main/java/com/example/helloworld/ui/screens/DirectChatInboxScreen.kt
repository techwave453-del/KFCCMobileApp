package com.example.helloworld.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.helloworld.data.ChatMemberSearchResult
import com.example.helloworld.data.ChatRoom
import com.example.helloworld.data.DirectChatRepository
import com.example.helloworld.data.DirectInboxItem
import com.example.helloworld.data.DirectMessage
import com.example.helloworld.data.StoryFeedItem
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import com.example.helloworld.ui.components.CopyableErrorMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.collect
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private enum class NewChatAction { MESSAGE, CONTACT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatInboxScreen(
    onOpenCommunity: () -> Unit,
    onOpenRoom: (String) -> Unit,
    onOpenAssistant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repository = remember { DirectChatRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var inbox by remember { mutableStateOf<List<DirectInboxItem>>(emptyList()) }
    var unreadCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var rooms by remember { mutableStateOf<List<ChatRoom>>(emptyList()) }
    var members by remember { mutableStateOf<List<ChatMemberSearchResult>>(emptyList()) }
    var stories by remember { mutableStateOf<List<StoryFeedItem>>(emptyList()) }
    var messages by remember { mutableStateOf<List<DirectMessage>>(emptyList()) }
    var selected by remember { mutableStateOf<DirectInboxItem?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf<NewChatAction?>(null) }
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<ChatMemberSearchResult>>(emptyList()) }
    var searchLoading by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedStory by remember { mutableStateOf<StoryFeedItem?>(null) }
    var storySequence by remember { mutableStateOf<List<StoryFeedItem>>(emptyList()) }
    var viewedStoryIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var storyProgress by remember { mutableFloatStateOf(0f) }
    var storyVideoDurationMs by remember { mutableLongStateOf(0L) }
    var storyVideoEnded by remember { mutableStateOf(false) }
    var storyMediaUrl by remember { mutableStateOf<String?>(null) }
    var preloadedStoryUrls by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var storyImageReady by remember { mutableStateOf(false) }
    var storyMediaLoading by remember { mutableStateOf(false) }
    var storyMediaError by remember { mutableStateOf<String?>(null) }
    var storyNotice by remember { mutableStateOf<String?>(null) }
    var pendingStoryBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingStoryMimeType by remember { mutableStateOf("") }
    var storyCaption by remember { mutableStateOf("") }
    var storyUploading by remember { mutableStateOf(false) }

    val storyPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val mimeType = context.contentResolver.getType(uri).orEmpty()
                val declaredSize = runCatching {
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                }.getOrDefault(-1L)
                val bytes = if (declaredSize > 50L * 1024L * 1024L) null else runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
                if (declaredSize > 50L * 1024L * 1024L) {
                    error = "Stories must be 50 MB or smaller."
                } else if (bytes == null) {
                    error = "Could not read the selected media file."
                } else {
                    pendingStoryBytes = bytes
                    pendingStoryMimeType = mimeType
                    storyCaption = ""
                }
            }
        }
    }

    fun openMemberConversation(member: ChatMemberSearchResult) {
        scope.launch {
            repository.startConversation(member.userId).onSuccess { id ->
                val row = DirectInboxItem(
                    conversationId = id,
                    peerUserId = member.userId,
                    peerUsername = member.username,
                    peerDisplayName = member.displayName,
                    peerAvatarUrl = member.avatarUrl,
                    lastMessagePreview = null,
                    lastMessageAt = Instant.now().toString()
                )
                inbox = listOf(row) + inbox.filterNot { it.conversationId == id }
                selected = row
                messages = emptyList()
                error = null
            }.onFailure { error = it.message ?: "Could not start conversation." }
        }
    }

    val inboxEntries = remember(inbox, rooms) {
        val roomEntries = rooms.map { InboxDisplayEntry(key = "room:" + it.id, room = it) }
        val communityFallback = if (rooms.none { it.type.equals("community", ignoreCase = true) }) {
            listOf(
                InboxDisplayEntry(
                    key = "room:community-fallback",
                    room = ChatRoom(id = "", type = "community", title = "Community Chat"),
                    communityFallback = true
                )
            )
        } else emptyList()
        roomEntries + communityFallback +
            inbox.map { InboxDisplayEntry(key = "direct:" + it.conversationId, direct = it) }
    }

    fun refresh() {
        scope.launch {
            loading = true
            error = null
            val inboxResult = repository.getInbox()
            inboxResult.onSuccess { inbox = it }.onFailure { error = it.message ?: "Could not load conversations." }
            repository.getUnreadCounts().onSuccess { unreadCounts = it }
            repository.getRooms().onSuccess { rooms = it }
                .onFailure { error = it.message ?: "Could not load group conversations." }
            repository.getMemberSuggestions().onSuccess { members = it }
                .onFailure { /* Keep chat usable even if suggestions cannot be loaded. */ }
            repository.getStoryFeed().onSuccess { stories = it }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    LaunchedEffect(stories) {
        // Warm the image cache for the first stories users are most likely to open.
        // Video URLs are signed on demand to avoid pre-downloading large files.
        val candidates = stories.filter { !it.mediaType.equals("video", ignoreCase = true) }.take(12)
        candidates.forEach { candidate ->
            if (preloadedStoryUrls[candidate.id] == null) {
                repository.getStoryMediaUrl(candidate.mediaPath).onSuccess { url ->
                    preloadedStoryUrls = preloadedStoryUrls + (candidate.id to url)
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .memoryCacheKey(url)
                        .diskCacheKey(url)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                    context.imageLoader.enqueue(request)
                }
            }
        }
    }


    LaunchedEffect(selected?.conversationId) {
        val conversation = selected ?: return@LaunchedEffect
        repository.markConversationRead(conversation.conversationId)
            .onSuccess { unreadCounts = unreadCounts - conversation.conversationId }
        suspend fun loadThread() {
            val result = repository.getMessages(conversation.conversationId)
            result.onSuccess { messages = it }
                .onFailure { if (messages.isEmpty()) error = it.message ?: "Could not load messages." }
            // Advance the receipt after successfully loading the visible thread, including new realtime messages.
            if (result.isSuccess) repository.markConversationRead(conversation.conversationId)
            repository.getUnreadCounts().onSuccess { unreadCounts = it - conversation.conversationId }
        }
        loadThread()

        // Realtime events are a refresh signal, never the sole source of truth.
        // Periodic re-fetching remains as a fallback for dropped events/offline reconnects.
        val realtimeChannel = SupabaseProvider.client.realtime.channel("direct-messages-${conversation.conversationId}")
        val changeFlow = realtimeChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "chat_direct_messages"
        }
        val realtimeJob = launch {
            runCatching {
                changeFlow.collect { loadThread() }
            }
        }
        try {
            runCatching { realtimeChannel.subscribe(blockUntilSubscribed = true) }
            while (isActive) {
                delay(15_000)
                loadThread()
            }
        } finally {
            realtimeJob.cancel()
            runCatching { SupabaseProvider.client.realtime.removeChannel(realtimeChannel) }
        }
    }

    LaunchedEffect(selected == null) {
        if (selected != null) return@LaunchedEffect
        while (isActive) {
            delay(15_000)
            repository.getInbox().onSuccess { inbox = it }
            repository.getUnreadCounts().onSuccess { unreadCounts = it }
        }
    }

    LaunchedEffect(selectedStory?.id) {
        val story = selectedStory ?: run {
            storyMediaUrl = null
            storyMediaError = null
            return@LaunchedEffect
        }
        storyMediaUrl = null
        storyMediaError = null
        storyProgress = 0f
        storyVideoDurationMs = 0L
        storyVideoEnded = false
        storyImageReady = false
        storyMediaLoading = true
        val cachedUrl = preloadedStoryUrls[story.id]
        val urlResult = if (cachedUrl != null) Result.success(cachedUrl) else repository.getStoryMediaUrl(story.mediaPath)
        urlResult
            .onSuccess { storyMediaUrl = it }
            .onFailure { storyMediaError = it.message ?: "Could not load story media." }
        storyMediaLoading = false
        repository.markStoryViewed(story.id)
        viewedStoryIds = viewedStoryIds + story.id

        if (story.mediaType.equals("video", ignoreCase = true)) {
            // Follow ExoPlayer's duration/end events; retain a timeout for stalled media.
            var elapsedMs = 0L
            while (elapsedMs < 30_000L && !storyVideoEnded) {
                delay(100)
                elapsedMs += 100
                val durationMs = storyVideoDurationMs.takeIf { it > 0L } ?: 30_000L
                storyProgress = (elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f)
            }
        } else {
            // Start the five-second viewing period only after Coil confirms the image is ready.
            var waitMs = 0L
            while (!storyImageReady && storyMediaError == null && waitMs < 15_000L) {
                delay(100)
                waitMs += 100
            }
            if (storyImageReady) {
                repeat(100) { step ->
                    delay(50)
                    storyProgress = (step + 1) / 100f
                }
            } else {
                // Avoid getting stuck indefinitely if the image cannot be loaded.
                delay(1_000)
            }
        }
        val currentIndex = storySequence.indexOfFirst { it.id == story.id }
        if (currentIndex >= 0 && currentIndex < storySequence.lastIndex) {
            selectedStory = storySequence[currentIndex + 1]
        } else {
            selectedStory = null
            storySequence = emptyList()
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        if (selected == null) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Chats",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onOpenAssistant) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Open Kanisa Assistant")
                    }
                    IconButton(onClick = { refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh inbox")
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Create chat, contact, community or story")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("New Chat") },
                                leadingIcon = { Icon(Icons.Default.Chat, null) },
                                onClick = { menuExpanded = false; action = NewChatAction.MESSAGE; searchText = ""; searchResults = emptyList() }
                            )
                            DropdownMenuItem(
                                text = { Text("New Contact") },
                                leadingIcon = { Icon(Icons.Default.Add, null) },
                                onClick = { menuExpanded = false; action = NewChatAction.CONTACT; searchText = ""; searchResults = emptyList() }
                            )
                            DropdownMenuItem(
                                text = { Text("New Community") },
                                leadingIcon = { Icon(Icons.Default.Groups, null) },
                                onClick = { menuExpanded = false; onOpenCommunity() }
                            )
                            DropdownMenuItem(
                                text = { Text("Add Story") },
                                leadingIcon = { Icon(Icons.Default.Add, null) },
                                onClick = { menuExpanded = false; storyPicker.launch(arrayOf("image/*", "video/*")) }
                            )
                        }
                    }
                }
            }

            Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("People", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { action = NewChatAction.MESSAGE; searchText = ""; searchResults = emptyList() }) {
                        Text("Find people")
                    }
                }
                if (members.isEmpty()) {
                    Text(
                        if (loading) "Loading members…" else "Search for a member to start chatting.",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(members, key = { it.userId }) { member ->
                            Column(
                                modifier = Modifier.width(64.dp).clickable { openMemberConversation(member) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.size(54.dp).clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))
                                        .padding(2.dp)
                                ) {
                                    MemberAvatar(
                                        member.avatarUrl,
                                        member.displayName?.takeIf { it.isNotBlank() } ?: member.username,
                                        50.dp
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    member.displayName?.takeIf { it.isNotBlank() } ?: member.username,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Keep the inbox focused: do not reserve space for an empty Stories section.
            if (stories.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp)) {
                    Text(
                        "Stories",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
                    )
                    val storiesByUser = stories
                        .groupBy { it.userId }
                        .mapNotNull { (_, userStories) -> userStories.maxByOrNull { it.createdAt } }
                        .sortedByDescending { it.createdAt }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(storiesByUser, key = { it.userId }) { story ->
                            Column(
                                modifier = Modifier.width(64.dp).clickable {
                                    storySequence = stories.filter { it.userId == story.userId }.sortedBy { it.createdAt }
                                    selectedStory = story
                                },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.size(54.dp).clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                if (story.id in viewedStoryIds) listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant)
                                                else listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                            )
                                        )
                                        .padding(3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize().padding(1.dp)) {
                                        if (!story.avatarUrl.isNullOrBlank()) {
                                            AsyncImage(model = story.avatarUrl, contentDescription = story.displayName ?: story.username, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                        } else {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(story.username.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    story.displayName?.takeIf { it.isNotBlank() } ?: story.username,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Messages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${inboxEntries.size} chats", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when {
                loading && inboxEntries.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                inboxEntries.isEmpty() -> EmptyInbox(onNewChat = { action = NewChatAction.MESSAGE; searchText = ""; searchResults = emptyList() }, onCommunity = onOpenCommunity)
                else -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(inboxEntries, key = { it.key }) { entry ->
                        val room = entry.room
                        if (room != null) {
                            ChatRoomInboxRow(
                                room = room,
                                onClick = {
                                    if (entry.communityFallback) onOpenCommunity() else onOpenRoom(room.id)
                                }
                            )
                        } else {
                            val item = entry.direct!!
                            InboxConversationRow(item = item, unreadCount = unreadCounts[item.conversationId] ?: 0, onClick = {
                                selected = item
                                messages = emptyList()
                                error = null
                            })
                        }
                    }
                    if (inbox.isEmpty()) {
                        item {
                            EmptyInbox(
                                onNewChat = { action = NewChatAction.MESSAGE; searchText = ""; searchResults = emptyList() },
                                onCommunity = onOpenCommunity
                            )
                        }
                    }
                }
            }
        } else {
            DirectMessageThread(
                item = selected!!,
                messages = messages,
                draft = draft,
                sending = sending,
                onDraftChange = { draft = it.take(4000) },
                onBack = { selected = null; messages = emptyList(); draft = "" },
                onSend = {
                    val text = draft.trim()
                    if (text.isNotEmpty() && !sending) {
                        scope.launch {
                            sending = true
                            repository.sendMessage(selected!!.conversationId, text, UUID.randomUUID().toString())
                                .onSuccess {
                                    draft = ""
                                    repository.getMessages(selected!!.conversationId).onSuccess { messages = it }
                                    refresh()
                                }
                                .onFailure { error = it.message ?: "Message could not be sent." }
                            sending = false
                        }
                    }
                }
            )
        }
    }

    if (action != null) {
        AlertDialog(
            onDismissRequest = { action = null; searchResults = emptyList(); searchText = "" },
            title = { Text(if (action == NewChatAction.MESSAGE) "Start a conversation" else "Add a contact") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { value ->
                            searchText = value
                            if (value.trim().length >= 2) {
                                scope.launch {
                                    searchLoading = true
                                    repository.searchMembers(value)
                                        .onSuccess { searchResults = it }
                                        .onFailure { error = it.message ?: "Member search failed." }
                                    searchLoading = false
                                }
                            } else searchResults = emptyList()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search by name or username") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    if (searchLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (searchText.trim().length < 2) {
                        Text("Enter at least two characters to search members.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    searchResults.forEach { member ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                val currentAction = action
                                scope.launch {
                                    if (currentAction == NewChatAction.CONTACT) {
                                        repository.addContact(member.userId)
                                            .onSuccess { storyNotice = "${member.displayName ?: member.username} added to contacts." }
                                            .onFailure { error = it.message ?: "Could not add contact." }
                                        action = null
                                    } else {
                                        action = null
                                        openMemberConversation(member)
                                    }
                                }
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MemberAvatar(member.avatarUrl, member.displayName ?: member.username, 42.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(member.displayName?.takeIf { it.isNotBlank() } ?: member.username, fontWeight = FontWeight.SemiBold)
                                Text("@${member.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(if (action == NewChatAction.CONTACT) "Add" else "Message", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (!searchLoading && searchText.trim().length >= 2 && searchResults.isEmpty()) {
                        Text("No matching members found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { action = null; searchText = ""; searchResults = emptyList() }) { Text("Done") } }
        )
    }

    if (pendingStoryBytes != null) {
        AlertDialog(
            onDismissRequest = { if (!storyUploading) { pendingStoryBytes = null; storyCaption = "" } },
            title = { Text("Add story caption") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = storyCaption,
                        onValueChange = { if (it.length <= 500) storyCaption = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Caption (optional)") },
                        supportingText = { Text(storyCaption.length.toString() + "/500") },
                        enabled = !storyUploading,
                        maxLines = 4
                    )
                    if (storyUploading) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("Uploading story… please keep the app open.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !storyUploading, onClick = {
                    val bytes = pendingStoryBytes ?: return@TextButton
                    storyUploading = true
                    scope.launch {
                        repository.publishStory(bytes, pendingStoryMimeType, storyCaption.trim())
                            .onSuccess {
                                storyNotice = "Your story was posted and will expire in 24 hours."
                                pendingStoryBytes = null
                                storyCaption = ""
                                repository.getStoryFeed().onSuccess { stories = it }
                            }
                            .onFailure { error = it.message ?: "Could not publish story." }
                        storyUploading = false
                    }
                }) { Text(if (storyUploading) "Uploading…" else "Publish") }
            },
            dismissButton = { TextButton(enabled = !storyUploading, onClick = { pendingStoryBytes = null; storyCaption = "" }) { Text("Cancel") } }
        )
    }
    selectedStory?.let { story ->
        val storyIndex = storySequence.indexOfFirst { it.id == story.id }.coerceAtLeast(0)
        Dialog(
            onDismissRequest = { selectedStory = null; storySequence = emptyList() },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    storySequence.forEachIndexed { index, _ ->
                        LinearProgressIndicator(
                            progress = { if (index < storyIndex) 1f else if (index == storyIndex) storyProgress else 0f },
                            modifier = Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(4.dp)),
                            color = androidx.compose.ui.graphics.Color.White,
                            trackColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.28f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MemberAvatar(story.avatarUrl, story.displayName ?: story.username, 38.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(story.displayName?.takeIf { it.isNotBlank() } ?: story.username, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.SemiBold)
                        Text(formatChatTime(story.createdAt), color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = { selectedStory = null; storySequence = emptyList() }) {
                        Text("Close", color = androidx.compose.ui.graphics.Color.White)
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when {
                        storyMediaLoading -> CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White)
                        storyMediaUrl != null -> StoryMediaContent(
                            mediaUrl = storyMediaUrl!!,
                            mediaType = story.mediaType,
                            modifier = Modifier.fillMaxSize(),
                            onVideoDuration = { duration -> storyVideoDurationMs = duration },
                            onVideoEnded = { storyVideoEnded = true },
                            onImageReady = { storyImageReady = true }
                        )
                        else -> Text(storyMediaError ?: "Story media is unavailable.", color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.padding(24.dp))
                    }
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxHeight().clickable {
                            if (storyIndex > 0) selectedStory = storySequence[storyIndex - 1]
                        })
                        Box(Modifier.weight(1f).fillMaxHeight().clickable {
                            if (storyIndex < storySequence.lastIndex) selectedStory = storySequence[storyIndex + 1]
                            else { selectedStory = null; storySequence = emptyList() }
                        })
                    }
                }
                if (story.caption.isNotBlank()) {
                    Text(story.caption, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.65f)).padding(horizontal = 18.dp, vertical = 14.dp))
                }
                Text("Tap left or right to navigate • Stories expire after 24 hours", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
    }

    storyNotice?.let { notice ->
        AlertDialog(onDismissRequest = { storyNotice = null }, title = { Text("Kanisa Chat") }, text = { Text(notice) }, confirmButton = { TextButton(onClick = { storyNotice = null }) { Text("OK") } })
    }
    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("Chat") },
            text = {
                CopyableErrorMessage(
                    message = message,
                    technicalDetails = message,
                    title = "Chat error"
                )
            },
            confirmButton = { TextButton(onClick = { error = null }) { Text("Close") } }
        )
    }
}


@Composable
private fun StoryMediaContent(
    mediaUrl: String,
    mediaType: String,
    modifier: Modifier = Modifier,
    onVideoDuration: (Long) -> Unit = {},
    onVideoEnded: () -> Unit = {},
    onImageReady: () -> Unit = {}
) {
    if (mediaType.equals("video", ignoreCase = true)) {
        val context = LocalContext.current
        val currentOnVideoDuration by rememberUpdatedState(onVideoDuration)
        val currentOnVideoEnded by rememberUpdatedState(onVideoEnded)
        val player = remember(mediaUrl) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(mediaUrl))
                prepare()
                playWhenReady = true
            }
        }
        DisposableEffect(player) {
            val listener = object : androidx.media3.common.Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == androidx.media3.common.Player.STATE_READY && player.duration > 0L) {
                        currentOnVideoDuration(player.duration)
                    }
                    if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                        currentOnVideoEnded()
                    }
                }
            }
            player.addListener(listener)
            if (player.duration > 0L) currentOnVideoDuration(player.duration)
            onDispose {
                player.removeListener(listener)
                player.release()
            }
        }
        AndroidView(
            factory = { viewContext -> PlayerView(viewContext).apply { this.player = player; useController = false } },
            update = { it.player = player },
            modifier = modifier.clip(RoundedCornerShape(8.dp))
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(mediaUrl)
                .memoryCacheKey(mediaUrl)
                .diskCacheKey(mediaUrl)
                .crossfade(false)
                .build(),
            contentDescription = "Story media",
            contentScale = ContentScale.Fit,
            onSuccess = { onImageReady() },
            modifier = modifier.clip(RoundedCornerShape(8.dp))
        )
    }
}

private data class InboxDisplayEntry(
    val key: String,
    val direct: DirectInboxItem? = null,
    val room: ChatRoom? = null,
    val communityFallback: Boolean = false,
)

@Composable
private fun ChatRoomInboxRow(room: ChatRoom, onClick: () -> Unit) {
    val isCommunity = room.type.equals("community", ignoreCase = true)
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(54.dp),
            shape = CircleShape,
            color = if (isCommunity) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isCommunity) Icons.Default.Groups else Icons.Default.Group,
                    contentDescription = null,
                    tint = if (isCommunity) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(room.title.ifBlank { if (isCommunity) "Community Chat" else "Group Chat" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(
                if (isCommunity) "Church community conversation" else "Group conversation",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "Open room", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InboxConversationRow(item: DirectInboxItem, unreadCount: Int = 0, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MemberAvatar(item.peerAvatarUrl, item.peerDisplayName ?: item.peerUsername, 54.dp)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(item.peerDisplayName?.takeIf { it.isNotBlank() } ?: item.peerUsername, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(item.lastMessagePreview?.takeIf { it.isNotBlank() } ?: "Start your conversation", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(formatChatTime(item.lastMessageAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(7.dp))
            if (unreadCount > 0) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)) {
                        Text(unreadCount.coerceAtMost(99).let { if (it == 99) "99+" else it.toString() }, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Spacer(Modifier.size(8.dp))
            }
        }
    }
}

@Composable
private fun MemberAvatar(url: String?, label: String, size: androidx.compose.ui.unit.Dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(model = url, contentDescription = label, modifier = Modifier.fillMaxSize().clip(CircleShape))
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(label.trim().take(1).uppercase().ifBlank { "K" }, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 18.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DirectMessageThread(
    item: DirectInboxItem,
    messages: List<DirectMessage>,
    draft: String,
    sending: Boolean,
    onDraftChange: (String) -> Unit,
    onBack: () -> Unit,
    onSend: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back to inbox") }
            MemberAvatar(item.peerAvatarUrl, item.peerDisplayName ?: item.peerUsername, 36.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(item.peerDisplayName?.takeIf { it.isNotBlank() } ?: item.peerUsername, fontWeight = FontWeight.Bold)
                Text("@${item.peerUsername}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider()
        if (messages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Say hello and start the conversation.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                reverseLayout = false
            ) {
                items(messages, key = { it.id }) { message ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(topStart = 5.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Column(Modifier.padding(horizontal = 11.dp, vertical = 7.dp)) {
                                Text(message.body, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(2.dp))
                                Text(formatChatTime(message.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().imePadding().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message…") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            Spacer(Modifier.width(6.dp))
            FilledIconButton(onClick = onSend, enabled = draft.isNotBlank() && !sending) {
                if (sending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Send, contentDescription = "Send message")
            }
        }
    }
}

@Composable
private fun EmptyInbox(onNewChat: () -> Unit, onCommunity: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(82.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary) }
        }
        Spacer(Modifier.height(16.dp))
        Text("Your conversations start here", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Message a member privately or join the church community conversation.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNewChat) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Start a chat") }
        TextButton(onClick = onCommunity) { Text("Open Community Chat") }
    }
}

private fun formatChatTime(value: String): String = runCatching {
    val instant = Instant.parse(value)
    val local = instant.atZone(ZoneId.systemDefault())
    val today = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
    if (local.toLocalDate() == today) local.format(DateTimeFormatter.ofPattern("HH:mm"))
    else local.format(DateTimeFormatter.ofPattern("d MMM"))
}.getOrDefault("")
