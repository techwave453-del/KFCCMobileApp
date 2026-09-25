package com.example.helloworld

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.helloworld.data.LocalCache
import com.example.helloworld.ui.ChurchViewModel
import com.example.helloworld.ui.ChatViewModel
import com.example.helloworld.admin.AdminViewModel
import com.example.helloworld.admin.AdminShell
import com.example.helloworld.ui.screens.*
import com.example.helloworld.ui.theme.KFCCTheme
import com.example.helloworld.ui.PreferencesViewModel
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.helloworld.data.LiveStream
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        LocalCache.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            val prefsViewModel: PreferencesViewModel = viewModel()
            val isDarkMode by prefsViewModel.isDarkMode.collectAsState()
            KFCCTheme(darkTheme = isDarkMode) { KFCCApp() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KFCCApp(
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
    val activity = LocalContext.current as? MainActivity

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("KFCC Menu", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { scope.launch { drawerState.close() } }) { Icon(Icons.Default.Close, "Close menu") }
                }
                HorizontalDivider()
                NavigationDrawerItem(label = { Text("Home") }, selected = currentDestination == AppDestinations.HOME, onClick = { navigate(AppDestinations.HOME) }, icon = { Icon(Icons.Default.Home, null) })
                NavigationDrawerItem(label = { Text("Bible") }, selected = currentDestination == AppDestinations.BIBLE, onClick = { navigate(AppDestinations.BIBLE) }, icon = { Icon(Icons.Filled.MenuBook, null) })
                
                NavigationDrawerItem(label = { Text("Notifications") }, selected = currentDestination == AppDestinations.NOTIFICATIONS, onClick = { navigate(AppDestinations.NOTIFICATIONS) }, icon = { Icon(Icons.Default.Notifications, null) })
                NavigationDrawerItem(label = { Text("Preferences") }, selected = currentDestination == AppDestinations.PREFERENCES, onClick = { navigate(AppDestinations.PREFERENCES) }, icon = { Icon(Icons.Default.Tune, null) })
                NavigationDrawerItem(label = { Text("Settings") }, selected = currentDestination == AppDestinations.SETTINGS, onClick = { navigate(AppDestinations.SETTINGS) }, icon = { Icon(Icons.Default.Settings, null) })
                NavigationDrawerItem(label = { Text("App Version") }, selected = currentDestination == AppDestinations.VERSION, onClick = { navigate(AppDestinations.VERSION) }, icon = { Icon(Icons.Default.Info, null) })

                if (adminUser != null) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        label = { Text("Admin Dashboard") },
                        selected = currentDestination == AppDestinations.ADMIN,
                        onClick = { navigate(AppDestinations.ADMIN) },
                        icon = { Icon(Icons.Default.AdminPanelSettings, null) }
                    )
                }

                if (chatSignedIn || adminUser != null) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        label = { Text("Sign out") },
                        selected = false,
                        onClick = {
                            chatViewModel.signOut()
                            adminViewModel.logout()
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = churchInfo.logoUrl.ifBlank { null }, contentDescription = churchInfo.churchName, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(churchInfo.churchName.ifBlank { "KFCC" })
                        }
                    },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, "Open menu") } }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = currentDestination == AppDestinations.HOME, onClick = { navigate(AppDestinations.HOME) }, icon = { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") })
                    NavigationBarItem(selected = currentDestination == AppDestinations.SEARCH, onClick = { navigate(AppDestinations.SEARCH) }, icon = { Icon(Icons.Default.Search, "Search") }, label = { Text("Search") })
                    NavigationBarItem(selected = currentDestination == AppDestinations.CHAT, onClick = { navigate(AppDestinations.CHAT) }, icon = { Icon(Icons.Default.Chat, "Chat") }, label = { Text("Chat") })
                    NavigationBarItem(
                        selected = currentDestination == AppDestinations.PROFILE || currentDestination == AppDestinations.ACCOUNT,
                        onClick = { navigate(if (chatSignedIn || adminUser != null) AppDestinations.PROFILE else AppDestinations.ACCOUNT) },
                        icon = { Icon(Icons.Default.AccountCircle, "Profile") },
                        label = { Text(if (chatSignedIn || adminUser != null) "Profile" else "Account") }
                    )
                }
            }
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
                    AppDestinations.CHAT -> ChatScreen(innerPadding, viewModel = chatViewModel, adminViewModel = adminViewModel, onAdminLoginSuccess = {
                        adminViewModel.restoreSession()
                        navigate(AppDestinations.ADMIN)
                    })
                    AppDestinations.ACCOUNT -> ChatScreen(innerPadding, viewModel = chatViewModel, adminViewModel = adminViewModel, onAdminLoginSuccess = {
                        adminViewModel.restoreSession()
                        navigate(AppDestinations.ADMIN)
                    })
                    AppDestinations.SEARCH -> SearchScreen(innerPadding)
                    AppDestinations.PROFILE -> ProfileScreen(innerPadding, adminViewModel = adminViewModel)
                    AppDestinations.NOTIFICATIONS -> NotificationsScreen(innerPadding)
                    AppDestinations.PREFERENCES -> PreferencesScreen(innerPadding)
                    AppDestinations.SETTINGS -> SettingsScreen(innerPadding)
                    AppDestinations.VERSION -> VersionScreen(innerPadding)
                    AppDestinations.GIVING -> GivingScreen(churchInfo, innerPadding)
                    AppDestinations.ADMIN -> AdminShell(adminViewModel, innerPadding, onBackToApp = { navigate(AppDestinations.HOME) })
                }
            }
        }
    }

    if (showLivePlayer) {
        LivePlayerDialog(liveStream = churchInfo.liveStream, onDismiss = { showLivePlayer = false })
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
                    } else {
                        Text("Live Player Placeholder\nURL: ${liveStream.url}", color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
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
    SERVICES("Services"),
    EVENTS("Events"),
    MEDIA("Media"),
    CHAT("Chat"),
    ACCOUNT("Account"),
    SEARCH("Search"),
    PROFILE("Profile"),
    NOTIFICATIONS("Notifications"),
    PREFERENCES("Preferences"),
    SETTINGS("Settings"),
    VERSION("Version"),
    GIVING("Giving"),
    ADMIN("Admin")
}



