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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.helloworld.ui.screens.bible.BibleChapterScreen
import com.example.helloworld.ui.screens.bible.BibleHomeScreen
import com.example.helloworld.ui.screens.bible.BibleSearchScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.helloworld.data.LiveStream
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {
    private var openNotifications by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()
        super.onCreate(savedInstanceState)
        handleNotificationIntent(intent)
        openNotifications = intent.getBooleanExtra(EXTRA_OPEN_NOTIFICATIONS, false)
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
                    onNotificationOpened = { openNotifications = false }
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
    }

    private fun handleNotificationIntent(intent: android.content.Intent?) {
        val notificationId = intent?.getStringExtra(EXTRA_NOTIFICATION_ID).orEmpty()
        if (notificationId.isBlank()) return

        // A notification opened from the Android tray is immediately considered read.
        NotificationManagerCompat.from(this).cancel(notificationId.hashCode())
        lifecycleScope.launch {
            NotificationRepository().markAsRead(notificationId)
        }
    }

    companion object {
        const val EXTRA_OPEN_NOTIFICATIONS = "kfcc.open_notifications"
        const val EXTRA_NOTIFICATION_ID = "kfcc.notification_id"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KFCCApp(
    openNotifications: Boolean = false,
    onNotificationOpened: () -> Unit = {},
    viewModel: ChurchViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory(LocalContext.current.applicationContext as Application)),
    adminViewModel: AdminViewModel = viewModel(factory = AdminViewModel.Factory(LocalContext.current.applicationContext as Application))
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var bibleBookId by rememberSaveable { mutableStateOf<String?>(null) }
    var bibleChapter by rememberSaveable { mutableStateOf(1) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    val churchInfo by viewModel.churchInfo.collectAsState()
    val mediaItems by viewModel.mediaItems.collectAsState()
    val events by viewModel.events.collectAsState()
    val chatSignedIn by chatViewModel.signedIn.collectAsState()
    val adminUser by adminViewModel.user.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showLivePlayer by remember { mutableStateOf(false) }
    var showBrandSplash by rememberSaveable { mutableStateOf(true) }
    val activity = LocalContext.current as? MainActivity
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        delay(1200)
        showBrandSplash = false
    }

    LaunchedEffect(openNotifications) {
        if (openNotifications) {
            currentDestination = AppDestinations.NOTIFICATIONS
            onNotificationOpened()
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(churchInfo.churchName) {
        activity?.title = churchInfo.churchName.ifBlank { "KFCC" }
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

    fun openBibleReference(reference: String) {
        val match = Regex(
            """^\s*(1\s+|2\s+|3\s+)?([A-Za-z]+(?:\s+[A-Za-z]+)?)\s+(\d+)(?::(\d+))?\s*$""",
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
        navigate(AppDestinations.BIBLE_CHAPTER)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(50.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (adminUser != null) adminUser!!.username else "My Profile",
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
                        IconButton(onClick = { navigate(AppDestinations.PROFILE) }) {
                            Icon(Icons.Default.ChevronRight, "Open profile")
                        }
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
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Open menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { navigate(AppDestinations.BIBLE) }) {
                            Icon(Icons.Default.MenuBook, "Bible")
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
                        onOpenEvents = { navigate(AppDestinations.EVENTS) },
                        onOpenGiving = { navigate(AppDestinations.GIVING) },
                        onOpenSermons = { navigate(AppDestinations.MEDIA) },
                        onOpenLive = { showLivePlayer = true },
                        onOpenServices = { navigate(AppDestinations.SERVICES) }
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
        KanisaBrandSplash()
    }

    if (showLivePlayer) {
        LivePlayerDialog(liveStream = churchInfo.liveStream, onDismiss = { showLivePlayer = false })
    }
}

@Composable
private fun KanisaBrandSplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF041B45),
                        Color(0xFF0B4B91),
                        Color(0xFFF0A83A),
                        Color(0xFF071B3A)
                    )
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                color = Color(0xFFFFC85A).copy(alpha = 0.9f),
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
            drawPath(far, Color(0xFF31527B).copy(alpha = 0.85f))

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
            drawPath(near, Color(0xFF071B3A))

            val crossX = w * 0.78f
            val crossTop = h * 0.46f
            val crossBottom = h * 0.72f
            drawRect(
                color = Color(0xFF2A2118),
                topLeft = androidx.compose.ui.geometry.Offset(crossX - w * 0.018f, crossTop),
                size = androidx.compose.ui.geometry.Size(w * 0.036f, crossBottom - crossTop)
            )
            drawRect(
                color = Color(0xFF2A2118),
                topLeft = androidx.compose.ui.geometry.Offset(crossX - w * 0.075f, h * 0.51f),
                size = androidx.compose.ui.geometry.Size(w * 0.15f, w * 0.032f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .align(Alignment.TopCenter)
                .padding(top = 118.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Kanisa",
                color = Color.White,
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.5).sp
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(42.dp).height(2.dp).background(Color(0xFFFFC12F)))
                Spacer(Modifier.width(12.dp))
                Text(
                    "Grow  •  Connect  •  Serve",
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(12.dp))
                Box(Modifier.width(42.dp).height(2.dp).background(Color(0xFFFFC12F)))
            }
        }

        Text(
            text = "Kingdom Fellowship Christian Church",
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 42.dp),
            textAlign = TextAlign.Center
        )
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
