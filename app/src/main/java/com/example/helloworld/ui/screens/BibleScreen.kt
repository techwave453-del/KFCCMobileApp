package com.example.helloworld.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage

private const val HERO_IMAGE_URL =
    "https://images.pexels.com/photos/53959/pexels-photo-53959.jpeg?auto=compress&cs=tinysrgb&w=1200"

// Set this to a church-owned MP4/HLS URL when one is available.
// When blank, the hero automatically uses HERO_IMAGE_URL.
private const val HERO_VIDEO_URL = ""

private val bibleBooks = listOf(
    BibleBook("Genesis", "Gen", 50, Testament.OLD),
    BibleBook("Exodus", "Exod", 40, Testament.OLD),
    BibleBook("Leviticus", "Lev", 27, Testament.OLD),
    BibleBook("Numbers", "Num", 36, Testament.OLD),
    BibleBook("Deuteronomy", "Deut", 34, Testament.OLD),
    BibleBook("Joshua", "Josh", 24, Testament.OLD),
    BibleBook("Judges", "Judg", 21, Testament.OLD),
    BibleBook("Ruth", "Ruth", 4, Testament.OLD),
    BibleBook("1 Samuel", "1 Sam", 31, Testament.OLD),
    BibleBook("2 Samuel", "2 Sam", 24, Testament.OLD),
    BibleBook("1 Kings", "1 Kgs", 22, Testament.OLD),
    BibleBook("2 Kings", "2 Kgs", 25, Testament.OLD),
    BibleBook("1 Chronicles", "1 Chr", 29, Testament.OLD),
    BibleBook("2 Chronicles", "2 Chr", 36, Testament.OLD),
    BibleBook("Ezra", "Ezra", 10, Testament.OLD),
    BibleBook("Nehemiah", "Neh", 13, Testament.OLD),
    BibleBook("Esther", "Esth", 10, Testament.OLD),
    BibleBook("Job", "Job", 42, Testament.OLD),
    BibleBook("Psalms", "Ps", 150, Testament.OLD),
    BibleBook("Proverbs", "Prov", 31, Testament.OLD),
    BibleBook("Ecclesiastes", "Eccl", 12, Testament.OLD),
    BibleBook("Song of Solomon", "Song", 8, Testament.OLD),
    BibleBook("Isaiah", "Isa", 66, Testament.OLD),
    BibleBook("Jeremiah", "Jer", 52, Testament.OLD),
    BibleBook("Lamentations", "Lam", 5, Testament.OLD),
    BibleBook("Ezekiel", "Ezek", 48, Testament.OLD),
    BibleBook("Daniel", "Dan", 12, Testament.OLD),
    BibleBook("Hosea", "Hos", 14, Testament.OLD),
    BibleBook("Joel", "Joel", 3, Testament.OLD),
    BibleBook("Amos", "Amos", 9, Testament.OLD),
    BibleBook("Obadiah", "Obad", 1, Testament.OLD),
    BibleBook("Jonah", "Jonah", 4, Testament.OLD),
    BibleBook("Micah", "Mic", 7, Testament.OLD),
    BibleBook("Nahum", "Nah", 3, Testament.OLD),
    BibleBook("Habakkuk", "Hab", 3, Testament.OLD),
    BibleBook("Zephaniah", "Zeph", 3, Testament.OLD),
    BibleBook("Haggai", "Hag", 2, Testament.OLD),
    BibleBook("Zechariah", "Zech", 14, Testament.OLD),
    BibleBook("Malachi", "Mal", 4, Testament.OLD),
    BibleBook("Matthew", "Matt", 28, Testament.NEW),
    BibleBook("Mark", "Mark", 16, Testament.NEW),
    BibleBook("Luke", "Luke", 24, Testament.NEW),
    BibleBook("John", "John", 21, Testament.NEW),
    BibleBook("Acts", "Acts", 28, Testament.NEW),
    BibleBook("Romans", "Rom", 16, Testament.NEW),
    BibleBook("1 Corinthians", "1 Cor", 16, Testament.NEW),
    BibleBook("2 Corinthians", "2 Cor", 13, Testament.NEW),
    BibleBook("Galatians", "Gal", 6, Testament.NEW),
    BibleBook("Ephesians", "Eph", 6, Testament.NEW),
    BibleBook("Philippians", "Phil", 4, Testament.NEW),
    BibleBook("Colossians", "Col", 4, Testament.NEW),
    BibleBook("1 Thessalonians", "1 Thess", 5, Testament.NEW),
    BibleBook("2 Thessalonians", "2 Thess", 3, Testament.NEW),
    BibleBook("1 Timothy", "1 Tim", 6, Testament.NEW),
    BibleBook("2 Timothy", "2 Tim", 4, Testament.NEW),
    BibleBook("Titus", "Titus", 3, Testament.NEW),
    BibleBook("Philemon", "Phlm", 1, Testament.NEW),
    BibleBook("Hebrews", "Heb", 13, Testament.NEW),
    BibleBook("James", "Jas", 5, Testament.NEW),
    BibleBook("1 Peter", "1 Pet", 5, Testament.NEW),
    BibleBook("2 Peter", "2 Pet", 3, Testament.NEW),
    BibleBook("1 John", "1 John", 5, Testament.NEW),
    BibleBook("2 John", "2 John", 1, Testament.NEW),
    BibleBook("3 John", "3 John", 1, Testament.NEW),
    BibleBook("Jude", "Jude", 1, Testament.NEW),
    BibleBook("Revelation", "Rev", 22, Testament.NEW)
)

private val bookAccents = listOf(
    Color(0xFF8B5E3C), Color(0xFF2E8064), Color(0xFF4B6CB7), Color(0xFFC28B2C),
    Color(0xFFAA5558), Color(0xFF397F94), Color(0xFF76529B)
)

private enum class Testament { OLD, NEW }

private data class BibleBook(
    val name: String,
    val abbreviation: String,
    val chapters: Int,
    val testament: Testament
)

private enum class BibleVersion(
    val title: String,
    val subtitle: String,
    val available: Boolean
) {
    ENGLISH_KJV("English", "King James Version", true),
    KISWAHILI_ULB("Kiswahili", "Swahili Unlocked Literal Bible", true),
    KIKAMBA("Kikamba", "Mbivilia — Bible Society of Kenya", false)
}

private data class Verse(
    val reference: String,
    val text: String,
    val version: BibleVersion
)

private val todayVerses = mapOf(
    BibleVersion.ENGLISH_KJV to Verse(
        "John 3:16",
        "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.",
        BibleVersion.ENGLISH_KJV
    ),
    BibleVersion.KISWAHILI_ULB to Verse(
        "Yohana 3:16",
        "Kwa maana jinsi hii Mungu aliupenda ulimwengu, kwamba akamtoa mwanae wa pekee, ili kwamba mtu yeyote amwaminiye asiangamie bali awe na uzima wa milele.",
        BibleVersion.KISWAHILI_ULB
    )
)

@Composable
fun BibleScreen(innerPadding: PaddingValues) {
    var selectedVersion by rememberSaveable { mutableStateOf(BibleVersion.ENGLISH_KJV.name) }
    var selectedBook by remember { mutableStateOf<BibleBook?>(null) }
    var selectedVerse by remember { mutableStateOf<Verse?>(null) }
    var showVersionPicker by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val version = BibleVersion.valueOf(selectedVersion)
    val todayVerse = todayVerses[version] ?: todayVerses.getValue(BibleVersion.ENGLISH_KJV)
    val filteredBooks = remember(searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) bibleBooks
        else bibleBooks.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.abbreviation.contains(query, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                BibleHero(
                    verse = todayVerse,
                    onVerseClick = { selectedVerse = todayVerse }
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Books of the Bible",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${bibleBooks.size} books • ${version.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { showVersionPicker = true },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Language, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(version.title)
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Search books") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    }
                )
            }

            item {
                Text(
                    "OLD TESTAMENT",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(filteredBooks.filter { it.testament == Testament.OLD }) { index, book ->
                BibleBookRow(
                    book = book,
                    accent = bookAccents[index % bookAccents.size],
                    onClick = { selectedBook = book }
                )
            }

            item {
                Text(
                    "NEW TESTAMENT",
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(filteredBooks.filter { it.testament == Testament.NEW }) { index, book ->
                BibleBookRow(
                    book = book,
                    accent = bookAccents[(index + 2) % bookAccents.size],
                    onClick = { selectedBook = book }
                )
            }
        }
    }

    if (showVersionPicker) {
        VersionPickerDialog(
            selected = version,
            onDismiss = { showVersionPicker = false },
            onSelected = {
                selectedVersion = it.name
                showVersionPicker = false
            }
        )
    }

    selectedBook?.let { book ->
        BibleBookDialog(
            book = book,
            version = version,
            onDismiss = { selectedBook = null },
            onVerseClick = { selectedVerse = it }
        )
    }

    selectedVerse?.let { verse ->
        VerseDetailDialog(verse = verse, onDismiss = { selectedVerse = null })
    }
}

@Composable
private fun BibleHero(verse: Verse, onVerseClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(390.dp)) {
        if (HERO_VIDEO_URL.isNotBlank()) {
            HeroVideo(HERO_VIDEO_URL)
        } else {
            AsyncImage(
                model = HERO_IMAGE_URL,
                contentDescription = "Bible hero background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.20f), Color.Black.copy(alpha = 0.76f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(9.dp).size(21.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "BIBLE",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Read the Word.",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .align(Alignment.BottomCenter)
                .clickable(onClick = onVerseClick),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.64f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "TODAY'S SCRIPTURE",
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = "Open scripture",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "“${verse.text}”",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 4
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    verse.reference.uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HeroVideo(url: String) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            PlayerView(it).apply {
                this.player = player
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            }
        },
        update = { it.player = player }
    )
}

@Composable
private fun BibleBookRow(book: BibleBook, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(16.dp),
            color = accent.copy(alpha = 0.18f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MenuBook, null, tint = accent, modifier = Modifier.size(25.dp))
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(book.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "${book.chapters} chapters",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(Icons.Default.ChevronRight, "Open ${book.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 88.dp, end = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    )
}

@Composable
private fun VersionPickerDialog(
    selected: BibleVersion,
    onDismiss: () -> Unit,
    onSelected: (BibleVersion) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text("Bible version", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Choose the language you want to read.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                BibleVersion.values().forEach { option ->
                    val isSelected = option == selected
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable(enabled = option.available) { onSelected(option) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        border = if (isSelected) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        } else null
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (option.available) Icons.Default.Language else Icons.Default.Lock,
                                null,
                                tint = if (option.available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(option.title, fontWeight = FontWeight.SemiBold)
                                Text(
                                    option.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun BibleBookDialog(
    book: BibleBook,
    version: BibleVersion,
    onDismiss: () -> Unit,
    onVerseClick: (Verse) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .padding(horizontal = 14.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 18.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.padding(start = 20.dp, top = 18.dp, end = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(book.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            version.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                }

                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Chapters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            (1..book.chapters).chunked(4).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    row.forEach { chapter ->
                                        OutlinedButton(
                                            onClick = { onVerseClick(verseForChapter(book, chapter, version)) },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 10.dp),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Text("$chapter")
                                        }
                                    }
                                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            "Tap a chapter to open its first verse.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun verseForChapter(book: BibleBook, chapter: Int, version: BibleVersion): Verse {
    if (book.name == "John" && chapter == 3 && version == BibleVersion.KISWAHILI_ULB) {
        return todayVerses.getValue(BibleVersion.KISWAHILI_ULB)
    }
    if (book.name == "John" && chapter == 3 && version == BibleVersion.ENGLISH_KJV) {
        return todayVerses.getValue(BibleVersion.ENGLISH_KJV)
    }
    return Verse(
        reference = "${book.name} $chapter:1",
        text = "This verse is ready for the selected translation. Connect the licensed Bible text source to load the complete chapter here.",
        version = version
    )
}

@Composable
private fun VerseDetailDialog(verse: Verse, onDismiss: () -> Unit) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = RoundedCornerShape(13.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.FormatQuote, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(verse.reference, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                verse.version.title + " • " + verse.version.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close verse") }
                    }

                    Spacer(Modifier.height(22.dp))

                    Text(
                        verse.text,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "${verse.reference}\n\n${verse.text}")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share verse"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Share")
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Done")
                        }
                    }

                    if (verse.version == BibleVersion.KISWAHILI_ULB) {
                        Text(
                            "Kiswahili Unlocked Literal Bible • © 2019 Door43 World Missions Community • CC BY-SA 4.0",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                        )
                    }
                }
            }
        }
    }
}
