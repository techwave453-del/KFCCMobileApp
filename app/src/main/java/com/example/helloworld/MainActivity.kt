package com.example.helloworld

import androidx.core.app.NotificationManagerCompat

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.helloworld.ui.screens.bible.BibleChapterScreen
import com.example.helloworld.ui.screens.bible.BibleHomeScreen
import com.example.helloworld.ui.screens.bible.BibleSearchScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.helloworld.data.LocalCache
import com.example.helloworld.data.NotificationRepository
import com.example.helloworld.ui.ChurchViewModel
import com.example.helloworld.ui.ChatViewModel
import com.example.helloworld.admin.AdminViewModel
import com.example.helloworld.admin.AdminShell
import com.example.helloworld.ui.screens.*
import com.example.helloworld.ui.theme.KFCCTheme
import com.example.helloworld.ui.PreferencesViewModel
import com.example.helloworld.ui.NotificationViewModel
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.helloworld.data.LiveStream
import com.example.helloworld.data.ChurchInfo
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.helloworld.updates.AppUpdateManager

class MainActivity : ComponentActivity() {
    private var openNotifications by mutableStateOf(false)
    private var openChatRoomId by mutableStateOf<String?>(null)
    private var openBibleReference by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()
        super.onCreate(savedInstanceState)
        handleNotificationIntent(intent)
        openNotifications = intent.getBooleanExtra(EXTRA_OPEN_NOTIFICATIONS, false)
        openChatRoomId = intent.getStringExtra(EXTRA_CHAT_ROOM_ID)?.takeIf { it.isNotBlank() }
        openBibleReference = intent.getStringExtra(EXTRA_BIBLE_REFERENCE)?.takeIf { it.isNotBlank() }
        LocalCache.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            val prefsViewModel: PreferencesViewModel = viewModel()
            val themeMode by prefsViewModel.themeMode.collectAsState()
            KFCCTheme(
                darkTheme = when (themeMode) {
                    "dark" -> true
                    "light" -> false
                    else -> androidx.compose.foundation.isSystemInDarkTheme()
                }
            ) {
                KFCCApp(
                    openNotifications = openNotifications,
                    openChatRoomId = openChatRoomId,
                    notificationBibleReference = openBibleReference,
                    onNotificationOpened = { openNotifications = false },
                    onChatOpened = { openChatRoomId = null },
                    onBibleReferenceOpened = { openBibleReference = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_NOTIFICATIONS, false)) {
            openNotifications = true
        }
        intent.getStringExtra(EXTRA_CHAT_ROOM_ID)?.takeIf { it.isNotBlank() }?.let {
            openChatRoomId = it
        }
        intent.getStringExtra(EXTRA_BIBLE_REFERENCE)?.takeIf { it.isNotBlank() }?.let {
            openBibleReference = it
        }
    }

    private fun handleNotificationIntent(intent: android.content.Intent?) {
        val notificationId = intent?.getStringExtra(EXTRA_NOTIFICATION_ID).orEmpty()
        if (notificationId.isNotBlank()) {
            NotificationManagerCompat.from(this).cancel(notificationId.hashCode())
            lifecycleScope.launch { NotificationRepository().markAsRead(notificationId) }
        }
    }

    companion object {
        const val EXTRA_OPEN_NOTIFICATIONS = "kfcc.open_notifications"
        const val EXTRA_BIBLE_REFERENCE = "kfcc.bible_reference"
        const val EXTRA_OPEN_CHAT = "kfcc.open_chat"
        const val EXTRA_CHAT_ROOM_ID = "kfcc.chat_room_id"
        const val EXTRA_NOTIFICATION_ID = "kfcc.notification_id"
    }
}

@Composable
private fun RequiredAppUpdateScreen(
    update: com.example.helloworld.admin.AppUpdateConfig,
    checking: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onInstall: () -> Unit
) {
    val context = LocalContext.current
    val downloadId = AppUpdateManager.getTrackedDownloadId(context)
    var downloadComplete by remember(downloadId) { mutableStateOf(false) }

    LaunchedEffect(downloadId, update.versionCode) {
        while (downloadId != -1L) {
            val manager = context.getSystemService(android.app.DownloadManager::class.java)
            val query = android.app.DownloadManager.Query().setFilterById(downloadId)
            manager.query(query).use { cursor ->
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(
                        cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_STATUS)
                    )
                    downloadComplete = status == android.app.DownloadManager.STATUS_SUCCESSFUL
                }
            }
            if (downloadComplete) break
            kotlinx.coroutines.delay(1000)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Required app update",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Kanisa ${update.versionName} is available. You must install this update before continuing to use the app.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            if (update.releaseNotes.isNotBlank()) {
                Text(
                    update.releaseNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(28.dp))

            when {
                downloadComplete -> {
                    Button(onClick = onInstall, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Install update")
                    }
                }
                checking -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Checking the required update…")
                }
                error != null -> {
                    Text(error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRetry) { Text("Retry") }
                }
                else -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Downloading required update…", textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "There is no Skip option. After installation, Kanisa will reopen on the new version.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RequiredUpdateCheckFailedScreen(
    message: String,
    onRetry: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Update verification required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Kanisa could not verify whether this installation is current. For security, the app will not continue until the update check succeeds.",
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KFCCApp(
    openNotifications: Boolean = false,
    openChatRoomId: String? = null,
    notificationBibleReference: String? = null,
    onNotificationOpened: () -> Unit = {},
    onChatOpened: () -> Unit = {},
    onBibleReferenceOpened: () -> Unit = {},
    viewModel: ChurchViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory(LocalContext.current.applicationContext as Application)),
    adminViewModel: AdminViewModel = viewModel(factory = AdminViewModel.Factory(LocalContext.current.applicationContext as Application)),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var pendingChatRoomId by rememberSaveable { mutableStateOf<String?>(null) }
    var bibleBookId by rememberSaveable { mutableStateOf<String?>(null) }
    var bibleChapter by rememberSaveable { mutableStateOf(1) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    val churchInfo by viewModel.churchInfo.collectAsState()
    val mediaItems by viewModel.mediaItems.collectAsState()
    val events by viewModel.events.collectAsState()
    val chatSignedIn by chatViewModel.signedIn.collectAsState()
    val adminUser by adminViewModel.user.collectAsState()
    val notifications by notificationViewModel.notifications.collectAsState()
    val unreadNotificationCount = remember(notifications) {
        notifications.count { it.readAt == null }
    }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showLivePlayer by remember { mutableStateOf(false) }
    var showBrandSplash by rememberSaveable { mutableStateOf(true) }
    val activity = LocalContext.current as? MainActivity
    val context = LocalContext.current
    val chatAuthRepository = remember { com.example.helloworld.data.ChatAuthRepository() }
    var menuProfile by remember { mutableStateOf<com.example.helloworld.data.ChatProfile?>(null) }

    fun openBibleReference(reference: String) {
        val match = Regex(
            """^\s*(1\s+|2\s+|3\s+)?([A-Za-z]+(?:\s+[A-Za-z]+)?)\s+(\d+)(?::(\d+)(?:[-–](\d+))?)?\s*$""",
            RegexOption.IGNORE_CASE
        ).matchEntire(reference.trim()) ?: return

        val bookName = listOfNotNull(
            match.groupValues[1].trim().takeIf { it.isNotBlank() },
            match.groupValues[2].trim().takeIf { it.isNotBlank() }
        ).joinToString(" ").lowercase()

        val bookId = bibleBookIdForName(bookName) ?: return
        val chapter = match.groupValues[3].toIntOrNull() ?: return
        bibleBookId = bookId
        bibleChapter = chapter
        bibleVerse = match.groupValues[4].toIntOrNull() ?: 0
        currentDestination = AppDestinations.BIBLE_CHAPTER
    }


    LaunchedEffect(Unit) {
        delay(1200)
        showBrandSplash = false
    }

    var mandatoryUpdate by remember { mutableStateOf<com.example.helloworld.admin.AppUpdateConfig?>(null) }
    var updateCheckFailed by remember { mutableStateOf(false) }
    var updateChecking by remember { mutableStateOf(true) }
    var updateError by remember { mutableStateOf<String?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    suspend fun checkMandatoryUpdate() {
        updateChecking = true
        updateError = null
        updateCheckFailed = false
        runCatching {
            AppUpdateManager.checkAndSchedule(context)
        }.onSuccess {
            mandatoryUpdate = it
            if (it != null) AppUpdateManager.installTrackedDownload(context)
        }.onFailure {
            updateError = it.message ?: "Unable to check for required app updates."
            updateCheckFailed = true
        }
        updateChecking = false
    }

    LaunchedEffect(Unit) {
        checkMandatoryUpdate()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { checkMandatoryUpdate() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(chatSignedIn, adminUser?.id) {
        if (chatSignedIn || adminUser != null) {
            notificationViewModel.onAuthenticated()
        }
    }

    LaunchedEffect(openNotifications) {
        if (openNotifications) {
            currentDestination = AppDestinations.NOTIFICATIONS
            onNotificationOpened()
        }
    }

    LaunchedEffect(notificationBibleReference) {
        val reference = notificationBibleReference ?: return@LaunchedEffect
        openBibleReference(reference)
        onBibleReferenceOpened()
    }

    LaunchedEffect(openChatRoomId, chatSignedIn, adminUser?.id) {
        val roomId = openChatRoomId ?: return@LaunchedEffect
        // Wait until the unified chat/admin session is restored. Otherwise a
        // notification tap during cold start can select the room before ChatViewModel
        // has an authenticated session and the navigation appears to do nothing.
        if (!chatSignedIn && adminUser == null) return@LaunchedEffect
        currentDestination = AppDestinations.CHAT
        chatViewModel.selectRoom(roomId)
        onChatOpened()
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            com.example.helloworld.notifications.KfccNotificationScheduler
                .scheduleInstallDelivery(context)
            com.example.helloworld.notifications.KfccNotificationScheduler
                .deliverDailyScriptureNow(context)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            com.example.helloworld.notifications.KfccNotificationScheduler
                .scheduleInstallDelivery(context)
            com.example.helloworld.notifications.KfccNotificationScheduler
                .deliverDailyScriptureNow(context)
        }
    }

    LaunchedEffect(churchInfo.churchName) {
        activity?.title = churchInfo.churchName.ifBlank { "KFCC" }
    }

    LaunchedEffect(drawerOpen, chatSignedIn, adminUser?.id) {
        if (drawerOpen || chatSignedIn || adminUser != null) {
            chatAuthRepository.getProfile().onSuccess { menuProfile = it }
        } else {
            menuProfile = null
        }
    }
    LaunchedEffect(drawerOpen) {
        if (drawerOpen) drawerState.open() else drawerState.close()
    }

    LaunchedEffect(drawerState.currentValue) {
        drawerOpen = drawerState.isOpen
    }

    fun navigate(destination: AppDestinations) {
        currentDestination = destination
        drawerOpen = false
    }

    fun backDestination(): AppDestinations = when (currentDestination) {
        AppDestinations.BIBLE_GAMES -> AppDestinations.HOME
        AppDestinations.BIBLE_GAME_QUIZ,
        AppDestinations.BIBLE_GAME_MEMORY_VERSE,
        AppDestinations.BIBLE_GAME_GUESS_CHARACTER,
        AppDestinations.BIBLE_GAME_FILL_IN_BLANK -> AppDestinations.BIBLE_GAMES
        AppDestinations.BIBLE_CHAPTER,
        AppDestinations.BIBLE_SEARCH -> AppDestinations.BIBLE
        AppDestinations.ACCOUNT -> AppDestinations.CHAT
        AppDestinations.PROFILE,
        AppDestinations.APPEARANCE,
        AppDestinations.NOTIFICATIONS,
        AppDestinations.PREFERENCES,
        AppDestinations.SETTINGS,
        AppDestinations.VERSION,
        AppDestinations.SEARCH,
        AppDestinations.SERVICES,
        AppDestinations.EVENTS,
        AppDestinations.MEDIA,
        AppDestinations.GIVING -> AppDestinations.HOME
        else -> AppDestinations.HOME
    }

    val showBackButton = currentDestination != AppDestinations.HOME &&
        currentDestination != AppDestinations.BIBLE &&
        currentDestination != AppDestinations.CHAT &&
        currentDestination != AppDestinations.ADMIN

    BackHandler(enabled = drawerOpen) {
        drawerOpen = false
    }

    BackHandler(enabled = !drawerOpen && showBackButton) {
        navigate(backDestination())
    }

    if (updateCheckFailed) {
        BackHandler(enabled = true) {}
        RequiredUpdateCheckFailedScreen(
            message = updateError ?: "Unable to verify the current app version.",
            onRetry = { scope.launch { checkMandatoryUpdate() } }
        )
        return
    }

    if (mandatoryUpdate != null) {
        BackHandler(enabled = true) {}

        RequiredAppUpdateScreen(
            update = mandatoryUpdate!!,
            checking = updateChecking,
            error = updateError,
            onRetry = { scope.launch { checkMandatoryUpdate() } },
            onInstall = { AppUpdateManager.installTrackedDownload(context) }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navigate(AppDestinations.PROFILE) }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(50.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (!menuProfile?.avatar_url.isNullOrBlank()) {
                                    AsyncImage(
                                        model = menuProfile?.avatar_url,
                                        contentDescription = "Profile picture",
                                        modifier = Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                adminUser?.username ?: menuProfile?.display_name ?: menuProfile?.username ?: "My Profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                if (adminUser != null) "Administrator" else "Community member",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Open profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider()

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        NavigationDrawerItem(label = { Text("Home") }, selected = currentDestination == AppDestinations.HOME, onClick = { navigate(AppDestinations.HOME) }, icon = { Icon(Icons.Default.Home, null) })
                        NavigationDrawerItem(label = { Text("Media Center") }, selected = currentDestination == AppDestinations.MEDIA, onClick = { navigate(AppDestinations.MEDIA) }, icon = { Icon(Icons.Default.PlayCircle, null) })
                        NavigationDrawerItem(label = { Text("Events") }, selected = currentDestination == AppDestinations.EVENTS, onClick = { navigate(AppDestinations.EVENTS) }, icon = { Icon(Icons.Default.Event, null) })
                        NavigationDrawerItem(label = { Text("Services") }, selected = currentDestination == AppDestinations.SERVICES, onClick = { navigate(AppDestinations.SERVICES) }, icon = { Icon(Icons.Default.Church, null) })
                        NavigationDrawerItem(label = { Text("Giving") }, selected = currentDestination == AppDestinations.GIVING, onClick = { navigate(AppDestinations.GIVING) }, icon = { Icon(Icons.Default.Favorite, null) })
                        NavigationDrawerItem(label = { Text("Bible") }, selected = currentDestination == AppDestinations.BIBLE, onClick = { navigate(AppDestinations.BIBLE) }, icon = { Icon(Icons.Default.MenuBook, null) })
                        NavigationDrawerItem(label = { Text("Chat") }, selected = currentDestination == AppDestinations.CHAT, onClick = { navigate(AppDestinations.CHAT) }, icon = { Icon(Icons.Default.Chat, null) })

                        if (adminUser != null) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            NavigationDrawerItem(
                                label = { Text("Administration") },
                                selected = currentDestination == AppDestinations.ADMIN,
                                onClick = { navigate(AppDestinations.ADMIN) },
                                icon = { Icon(Icons.Default.AdminPanelSettings, null) }
                            )
                        }

                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        NavigationDrawerItem(
                            label = { Text("Appearance") },
                            selected = currentDestination == AppDestinations.APPEARANCE,
                            onClick = { navigate(AppDestinations.APPEARANCE) },
                            icon = { Icon(Icons.Default.Palette, null) }
                        )
                        NavigationDrawerItem(
                            label = { Text("Settings") },
                            selected = currentDestination == AppDestinations.SETTINGS,
                            onClick = { navigate(AppDestinations.SETTINGS) },
                            icon = { Icon(Icons.Default.Settings, null) }
                        )
                        NavigationDrawerItem(
                            label = { Text("Notifications") },
                            selected = currentDestination == AppDestinations.NOTIFICATIONS,
                            onClick = { navigate(AppDestinations.NOTIFICATIONS) },
                            icon = { Icon(Icons.Default.Notifications, null) }
                        )
                        NavigationDrawerItem(
                            label = { Text("App Version") },
                            selected = currentDestination == AppDestinations.VERSION,
                            onClick = { navigate(AppDestinations.VERSION) },
                            icon = { Icon(Icons.Default.Info, null) }
                        )
                    }

                    if (chatSignedIn || adminUser != null) {
                        HorizontalDivider()
                        NavigationDrawerItem(
                            label = { Text("Log out") },
                            selected = false,
                            onClick = {
                                chatViewModel.signOut()
                                adminViewModel.logout()
                                navigate(AppDestinations.HOME)
                            },
                            icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) }
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            churchInfo.churchName.ifBlank { "KFCC" },
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (showBackButton) {
                                    navigate(backDestination())
                                } else {
                                    scope.launch { drawerState.open() }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (showBackButton) {
                                    Icons.Default.ArrowBack
                                } else {
                                    Icons.Default.Menu
                                },
                                contentDescription = if (showBackButton) "Back" else "Open menu"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { navigate(AppDestinations.NOTIFICATIONS) }) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationCount > 0) {
                                        Badge {
                                            Text(
                                                text = if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString()
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, "Notifications")
                            }
                        }
                        IconButton(onClick = { navigate(AppDestinations.SEARCH) }) {
                            Icon(Icons.Default.Search, "Search")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentDestination == AppDestinations.HOME,
                        onClick = { navigate(AppDestinations.HOME) },
                        icon = { Icon(Icons.Default.Home, "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentDestination == AppDestinations.BIBLE,
                        onClick = { navigate(AppDestinations.BIBLE) },
                        icon = { Icon(Icons.Default.MenuBook, "Bible") },
                        label = { Text("Bible") }
                    )
                    NavigationBarItem(
                        selected = currentDestination == AppDestinations.CHAT || currentDestination == AppDestinations.ACCOUNT,
                        onClick = { navigate(AppDestinations.CHAT) },
                        icon = { Icon(Icons.Default.Chat, "Chat") },
                        label = { Text("Chat") }
                    )
                    if (adminUser != null) {
                        NavigationBarItem(
                            selected = currentDestination == AppDestinations.ADMIN,
                            onClick = { navigate(AppDestinations.ADMIN) },
                            icon = { Icon(Icons.Default.AdminPanelSettings, "Admin") },
                            label = { Text("Admin") }
                        )
                    }
                }
            },
        ) { innerPadding ->
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                when (currentDestination) {
                    AppDestinations.HOME -> HomeScreen(
                        info = churchInfo,
                        mediaItems = mediaItems,
                        events = events,
                        innerPadding = innerPadding,
                        onOpenChat = { navigate(AppDestinations.CHAT) },
                        onOpenMedia = { navigate(AppDestinations.MEDIA) },
                        onOpenBible = { navigate(AppDestinations.BIBLE) },
                        onOpenBibleGames = { navigate(AppDestinations.BIBLE_GAMES) },
                        onOpenEvents = { navigate(AppDestinations.EVENTS) },
                        onOpenGiving = { navigate(AppDestinations.GIVING) },
                        onOpenSermons = { navigate(AppDestinations.MEDIA) },
                        onOpenLive = { showLivePlayer = true },
                        onOpenServices = { navigate(AppDestinations.SERVICES) }
                    )

                    AppDestinations.BIBLE_GAMES -> BibleGamesHubScreen(
                        onBack = { navigate(AppDestinations.HOME) },
                        onOpenQuiz = { navigate(AppDestinations.BIBLE_GAME_QUIZ) },
                        onOpenMemoryVerse = { navigate(AppDestinations.BIBLE_GAME_MEMORY_VERSE) },
                        onOpenGuessCharacter = { navigate(AppDestinations.BIBLE_GAME_GUESS_CHARACTER) },
                        onOpenFillInBlank = { navigate(AppDestinations.BIBLE_GAME_FILL_IN_BLANK) },
                        innerPadding = innerPadding
                    )

                    AppDestinations.BIBLE_GAME_QUIZ -> BibleGamesScreen(
                         onBack = { navigate(AppDestinations.BIBLE_GAMES) },
                         innerPadding = innerPadding
                     )
                     AppDestinations.BIBLE_GAME_MEMORY_VERSE -> MemoryVerseScreen(
                         onBack = { navigate(AppDestinations.BIBLE_GAMES) },
                         innerPadding = innerPadding
                     )
                     AppDestinations.BIBLE_GAME_GUESS_CHARACTER -> GuessCharacterScreen(
                          onBack = { navigate(AppDestinations.BIBLE_GAMES) },
                          onOpenReference = { openBibleReference(it) },
                          innerPadding = innerPadding
                      )
                     AppDestinations.BIBLE_GAME_FILL_IN_BLANK -> FillInBlankScreen(
                          onBack = { navigate(AppDestinations.BIBLE_GAMES) },
                          onOpenReference = { openBibleReference(it) },
                          innerPadding = innerPadding
                      )
                     AppDestinations.BIBLE -> BibleHomeScreen(
                        onBack = { navigate(AppDestinations.HOME) },
                        onOpenSearch = { navigate(AppDestinations.BIBLE_SEARCH) },
                        onOpenChapter = { bookId, chapter ->
                            bibleBookId = bookId
                            bibleChapter = chapter
                            navigate(AppDestinations.BIBLE_CHAPTER)
                        }
                    )

                    AppDestinations.BIBLE_SEARCH -> BibleSearchScreen(
                        onBack = { navigate(AppDestinations.BIBLE) },
                        onOpenChapter = { bookId, chapter ->
                            bibleBookId = bookId
                            bibleChapter = chapter
                            navigate(AppDestinations.BIBLE_CHAPTER)
                        }
                    )

                    AppDestinations.BIBLE_CHAPTER -> {

                        BibleChapterScreen(
                            bookId = bibleBookId ?: "psalms",
                            chapterNumber = bibleChapter,
                            initialVerse = bibleVerse.takeIf { it > 0 },
                            onBack = { navigate(AppDestinations.BIBLE) }
                        )

                    }
                    AppDestinations.SERVICES -> ServicesScreen(churchInfo.services, innerPadding)
                    AppDestinations.EVENTS -> EventsScreen(events, innerPadding)
                    AppDestinations.MEDIA -> MediaScreen(mediaItems, churchInfo.liveStream, innerPadding)
                    AppDestinations.CHAT -> ChatScreen(
                        innerPadding,
                        viewModel = chatViewModel,
                        adminViewModel = adminViewModel,
                        onAdminLoginSuccess = {
                            adminViewModel.restoreSession()
                            navigate(AppDestinations.ADMIN)
                        },
                        onOpenBibleReference = ::openBibleReference
                    )
                    AppDestinations.ACCOUNT -> ChatScreen(
                        innerPadding,
                        viewModel = chatViewModel,
                        adminViewModel = adminViewModel,
                        onAdminLoginSuccess = {
                            adminViewModel.restoreSession()
                            navigate(AppDestinations.ADMIN)
                        },
                        onOpenBibleReference = ::openBibleReference
                    )
                    AppDestinations.SEARCH -> SearchScreen(innerPadding)
                    AppDestinations.PROFILE -> ProfileScreen(innerPadding, adminViewModel = adminViewModel)
                    AppDestinations.APPEARANCE -> AppearanceScreen(innerPadding)
                    AppDestinations.NOTIFICATIONS -> NotificationsScreen(
                        innerPadding = innerPadding,
                        canViewNotifications = chatSignedIn || adminUser != null
                    )
                    AppDestinations.PREFERENCES -> PreferencesScreen(innerPadding)
                    AppDestinations.SETTINGS -> SettingsScreen(
                        innerPadding = innerPadding,
                        onOpenAppearance = { navigate(AppDestinations.APPEARANCE) },
                        onOpenNotifications = { navigate(AppDestinations.NOTIFICATIONS) }
                    )
                    AppDestinations.VERSION -> VersionScreen(innerPadding)
                    AppDestinations.GIVING -> GivingScreen(churchInfo, innerPadding)
                    AppDestinations.ADMIN -> AdminShell(adminViewModel, innerPadding, onBackToApp = { navigate(AppDestinations.HOME) })
                }
            }
        }
    }

    if (showBrandSplash) {
        KanisaBrandSplash(churchInfo)
    }

    if (showLivePlayer) {
        LivePlayerDialog(liveStream = churchInfo.liveStream, onDismiss = { showLivePlayer = false })
    }
}

@Composable
private fun KanisaBrandSplash(churchInfo: ChurchInfo) {
    val splash = churchInfo.splash
    val palette = when (splash.theme.lowercase()) {
        "royal" -> listOf(Color(0xFF061A4F), Color(0xFF0B55B7), Color(0xFF2B8BD8), Color(0xFF061A4F))
        "light" -> listOf(Color(0xFFF7FAFF), Color(0xFFE7F0FF), Color(0xFFFFD76A), Color(0xFFFFFFFF))
        "midnight" -> listOf(Color(0xFF020817), Color(0xFF0B1E3A), Color(0xFF163A66), Color(0xFF020817))
        else -> listOf(Color(0xFF041B45), Color(0xFF0B4B91), Color(0xFFF0A83A), Color(0xFF071B3A))
    }
    val foreground = if (splash.theme == "light") Color(0xFF071B3A) else Color.White
    val accent = Color(0xFFFFC12F)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(palette))
    ) {
        if (splash.backgroundImageUrl.isNotBlank()) {
            AsyncImage(
                model = splash.backgroundImageUrl,
                contentDescription = "Splash background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                palette[0].copy(alpha = 0.68f),
                                palette[1].copy(alpha = 0.42f),
                                palette[3].copy(alpha = 0.86f)
                            )
                        )
                    )
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    color = accent.copy(alpha = 0.88f),
                    radius = w * 0.11f,
                    center = androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.57f)
                )
                val far = Path().apply {
                    moveTo(0f, h * 0.64f)
                    lineTo(w * 0.20f, h * 0.57f)
                    lineTo(w * 0.36f, h * 0.63f)
                    lineTo(w * 0.56f, h * 0.54f)
                    lineTo(w * 0.76f, h * 0.62f)
                    lineTo(w, h * 0.55f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(far, Color(0xFF31527B).copy(alpha = if (splash.theme == "light") 0.35f else 0.85f))
                val near = Path().apply {
                    moveTo(0f, h * 0.74f)
                    lineTo(w * 0.22f, h * 0.66f)
                    lineTo(w * 0.42f, h * 0.75f)
                    lineTo(w * 0.66f, h * 0.63f)
                    lineTo(w, h * 0.72f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(near, Color(0xFF071B3A).copy(alpha = if (splash.theme == "light") 0.25f else 1f))
            }
        }

        // Keep the complete church identity block together in the visual center of the splash.
        // This makes the name, phrase, contact details, and official name easy to read at a glance.
        val details = splash.contactDetails.ifBlank {
            listOf(churchInfo.phone, churchInfo.email)
                .filter { it.isNotBlank() }
                .joinToString(" • ")
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (churchInfo.logoUrl.isNotBlank()) {
                AsyncImage(
                    model = churchInfo.logoUrl,
                    contentDescription = "Church logo",
                    modifier = Modifier.size(92.dp)
                )
                Spacer(Modifier.height(18.dp))
            }

            val churchName = churchInfo.churchName.ifBlank { "Kanisa" }
            val churchNameFontSize = when {
                churchName.length > 34 -> 30.sp
                churchName.length > 26 -> 34.sp
                churchName.length > 20 -> 38.sp
                else -> 42.sp
            }

            Text(
                text = churchName,
                color = foreground,
                fontSize = churchNameFontSize,
                lineHeight = (churchNameFontSize.value * 1.12f).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            if (splash.phrase.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(Modifier.width(36.dp).height(2.dp).background(accent))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        splash.phrase,
                        color = foreground.copy(alpha = 0.94f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.width(36.dp).height(2.dp).background(accent))
                }
            }

            if (splash.showContactDetails && details.isNotBlank()) {
                Spacer(Modifier.height(22.dp))
                Text(
                    text = details,
                    color = foreground.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = churchInfo.officialName.ifBlank { churchInfo.churchName },
                color = foreground.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LivePlayerDialog(liveStream: LiveStream, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, end = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        liveStream.title.ifBlank { "Live Worship Service" },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    if (liveStream.url.contains("youtube.com") || liveStream.url.contains("youtu.be")) {
                        val videoId = youtubeVideoId(liveStream.url)
                        if (videoId != null) {
                            YoutubePlayer(videoId = videoId)
                        } else {
                            Text("Invalid YouTube URL", color = Color.White, modifier = Modifier.align(Alignment.Center))
                        }
                    } else if (liveStream.url.isNotBlank()) {
                        DirectMediaPlayer(url = liveStream.url)
                    } else {
                        Text(
                            "No live stream URL has been configured.",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DirectMediaPlayer(url: String) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(PlayerMediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { PlayerView(it).apply { this.player = player } },
        update = { it.player = player }
    )
}

@Composable
private fun YoutubePlayer(videoId: String) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            YouTubePlayerView(context).also { playerView ->
                playerView.enableAutomaticInitialization = false
                lifecycleOwner.lifecycle.addObserver(playerView)
                playerView.initialize(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.loadVideo(videoId, 0f)
                    }
                }, true)
            }
        }
    )
}

private fun youtubeVideoId(url: String): String? {
    val patterns = listOf(
        Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/|youtube\\.com/live/)([A-Za-z0-9_-]{11})"),
        Regex("youtube\\.com/watch\\?.*v=([A-Za-z0-9_-]{11})")
    )
    return patterns.firstNotNullOfOrNull { it.find(url)?.groupValues?.getOrNull(1) }
}

enum class AppDestinations(val label: String) {
    HOME("Home"),
    BIBLE("Bible"),
    BIBLE_GAMES("Bible Games"),
    BIBLE_GAME_QUIZ("Bible Quiz"),
    BIBLE_GAME_MEMORY_VERSE("Memory Verse"),
    BIBLE_GAME_GUESS_CHARACTER("Guess the Character"),
    BIBLE_CHAPTER("Bible Chapter"),
    BIBLE_SEARCH("Bible Search"),
    SERVICES("Services"),
    EVENTS("Events"),
    MEDIA("Media"),
    CHAT("Chat"),
    ACCOUNT("Account"),
    SEARCH("Search"),
    PROFILE("Profile"),
    APPEARANCE("Appearance"),
    NOTIFICATIONS("Notifications"),
    PREFERENCES("Preferences"),
    SETTINGS("Settings"),
    VERSION("Version"),
    GIVING("Giving"),
    ADMIN("Admin")
}





private fun bibleBookIdForName(name: String): String? = when (name) {
    "genesis" -> "GEN"; "exodus" -> "EXO"; "leviticus" -> "LEV"; "numbers" -> "NUM"; "deuteronomy" -> "DEU"
    "joshua" -> "JOS"; "judges" -> "JDG"; "ruth" -> "RUT"; "1 samuel" -> "1SA"; "2 samuel" -> "2SA"
    "1 kings" -> "1KI"; "2 kings" -> "2KI"; "1 chronicles" -> "1CH"; "2 chronicles" -> "2CH"; "ezra" -> "EZR"
    "nehemiah" -> "NEH"; "esther" -> "EST"; "job" -> "JOB"; "psalm", "psalms" -> "PSA"; "proverbs" -> "PRO"
    "ecclesiastes" -> "ECC"; "song of solomon", "song of songs" -> "SNG"; "isaiah" -> "ISA"; "jeremiah" -> "JER"
    "lamentations" -> "LAM"; "ezekiel" -> "EZK"; "daniel" -> "DAN"; "hosea" -> "HOS"; "joel" -> "JOL"; "amos" -> "AMO"
    "obadiah" -> "OBA"; "jonah" -> "JON"; "micah" -> "MIC"; "nahum" -> "NAM"; "habakkuk" -> "HAB"; "zephaniah" -> "ZEP"
    "haggai" -> "HAG"; "zechariah" -> "ZEC"; "malachi" -> "MAL"; "matthew" -> "MAT"; "mark" -> "MRK"; "luke" -> "LUK"
    "john" -> "JHN"; "acts" -> "ACT"; "romans" -> "ROM"; "1 corinthians" -> "1CO"; "2 corinthians" -> "2CO"
    "galatians" -> "GAL"; "ephesians" -> "EPH"; "philippians" -> "PHP"; "colossians" -> "COL"; "1 thessalonians" -> "1TH"
    "2 thessalonians" -> "2TH"; "1 timothy" -> "1TI"; "2 timothy" -> "2TI"; "titus" -> "TIT"; "philemon" -> "PHM"
    "hebrews" -> "HEB"; "james" -> "JAS"; "1 peter" -> "1PE"; "2 peter" -> "2PE"; "1 john" -> "1JN"; "2 john" -> "2JN"
    "3 john" -> "3JN"; "jude" -> "JUD"; "revelation" -> "REV"; else -> null
}
