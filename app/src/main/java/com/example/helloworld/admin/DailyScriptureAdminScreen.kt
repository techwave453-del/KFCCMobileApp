package com.example.helloworld.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.SupabaseProvider
import com.example.helloworld.data.bible.BibleBook
import com.example.helloworld.data.bible.BibleTranslation
import com.example.helloworld.data.bible.KfccBibleRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable
import kotlinx.coroutines.launch

@Serializable
private data class ThemeRow(
    val id: String,
    val slug: String,
    val name: String,
    val description: String = "",
    val is_active: Boolean = true,
    val sort_order: Int = 0
)

@Serializable
private data class DailyScriptureSettingsRow(
    val id: Int,
    val selected_theme_id: String? = null
)

@Serializable
private data class EntryRow(
    val id: String,
    val theme_id: String,
    val translation_id: String,
    val book_id: String,
    val chapter: Int,
    val verse_start: Int,
    val verse_end: Int,
    val situation: String = "",
    val reflection: String = "",
    val priority: Int = 0,
    val is_active: Boolean = true
)

@Composable
fun AdminDailyScriptureScreen(
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val bibleRepository = remember { KfccBibleRepository() }
    var themes by remember { mutableStateOf<List<ThemeRow>>(emptyList()) }
    var entries by remember { mutableStateOf<List<EntryRow>>(emptyList()) }
    var books by remember { mutableStateOf<List<BibleBook>>(emptyList()) }
    var translations by remember { mutableStateOf<List<BibleTranslation>>(emptyList()) }
    var selectedThemeId by remember { mutableStateOf<String?>(null) }
    var themeMenuExpanded by remember { mutableStateOf(false) }
    var savingThemeChoice by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showEntryDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var themePendingDelete by remember { mutableStateOf<ThemeRow?>(null) }
    var entryPendingDelete by remember { mutableStateOf<EntryRow?>(null) }

    suspend fun reload() {
        loading = true
        error = null
        try {
            themes = SupabaseProvider.client.from("daily_scripture_themes").select {
                order("sort_order", Order.ASCENDING)
            }.decodeList()
            selectedThemeId = SupabaseProvider.client.from("daily_scripture_settings").select {
                filter { eq("id", 1) }
            }.decodeList<DailyScriptureSettingsRow>().firstOrNull()?.selected_theme_id
            entries = SupabaseProvider.client.from("daily_scriptures").select {
                order("priority", Order.DESCENDING)
            }.decodeList()
            translations = bibleRepository.getTranslations()
            val preferred = translations.firstOrNull { it.id.equals("kjv", true) } ?: translations.firstOrNull()
            books = preferred?.let { bibleRepository.getBooks(it.id) } ?: emptyList()
        } catch (e: Exception) {
            error = e.message ?: "Unable to load Today's Scripture."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Today's Scripture", style = MaterialTheme.typography.headlineSmall)
                Text("Organize daily Bible passages by real-life themes.", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = { scope.launch { reload() } }) {
                Icon(Icons.Default.Refresh, "Refresh")
            }
        }

        Spacer(Modifier.height(12.dp))

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Today's theme", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Choose a theme to pin for Today's Scripture. If you leave it on Random, Kanisa selects an active theme automatically for the day.",
                    style = MaterialTheme.typography.bodySmall
                )
                Box {
                    OutlinedTextField(
                        value = selectedThemeId?.let { id -> themes.firstOrNull { it.id == id }?.name } ?: "Random",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Theme selection") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = themeMenuExpanded,
                        onDismissRequest = { themeMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Random") },
                            onClick = {
                                selectedThemeId = null
                                themeMenuExpanded = false
                            }
                        )
                        themes.filter { it.is_active }.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name) },
                                onClick = {
                                    selectedThemeId = item.id
                                    themeMenuExpanded = false
                                }
                            )
                        }
                    }
                    Spacer(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { themeMenuExpanded = true }
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        enabled = !savingThemeChoice,
                        onClick = {
                            scope.launch {
                                savingThemeChoice = true
                                runCatching {
                                    SupabaseProvider.client.from("daily_scripture_settings")
                                        .update(mapOf("selected_theme_id" to selectedThemeId)) {
                                            filter { eq("id", 1) }
                                        }
                                    SupabaseProvider.client.from("daily_scripture_settings")
                                        .update(mapOf("updated_at" to java.time.Instant.now().toString())) {
                                            filter { eq("id", 1) }
                                        }
                                }.onFailure {
                                    error = it.message ?: "Unable to save Today's Scripture theme."
                                }
                                savingThemeChoice = false
                            }
                        }
                    ) {
                        Text(if (savingThemeChoice) "Saving..." else "Save theme choice")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showThemeDialog = true }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Theme")
            }
            Button(
                onClick = { showEntryDialog = true },
                enabled = themes.isNotEmpty() && books.isNotEmpty()
            ) {
                Icon(Icons.Default.MenuBook, null)
                Spacer(Modifier.width(6.dp))
                Text("Scripture")
            }
        }

        Spacer(Modifier.height(16.dp))
        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Themes", style = MaterialTheme.typography.titleMedium)
            }
            items(themes, key = { it.id }) { theme ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(theme.name, style = MaterialTheme.typography.titleMedium)
                                if (theme.description.isNotBlank()) Text(theme.description, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${entries.count { it.theme_id == theme.id }} Scripture entries",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    runCatching {
                                        SupabaseProvider.client.from("daily_scripture_themes").delete {
                                            filter { eq("id", theme.id) }
                                        }
                                        reload()
                                    }.onFailure { error = it.message ?: "Unable to delete theme." }
                                }
                            }) {
                                Icon(Icons.Default.Delete, "Delete theme")
                            }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text("Scripture entries", style = MaterialTheme.typography.titleMedium)
            }
            items(entries, key = { it.id }) { entry ->
                val themeName = themes.firstOrNull { it.id == entry.theme_id }?.name ?: "Unknown theme"
                val bookName = books.firstOrNull { it.id == entry.book_id }?.name ?: entry.book_id
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(themeName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "$bookName ${entry.chapter}:${entry.verse_start}" +
                                    if (entry.verse_end == entry.verse_start) "" else "-${entry.verse_end}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (entry.situation.isNotBlank()) Text(entry.situation, style = MaterialTheme.typography.bodySmall)
                            if (entry.reflection.isNotBlank()) Text(entry.reflection, style = MaterialTheme.typography.bodySmall)
                        }
                        Column {
                            Switch(checked = entry.is_active, onCheckedChange = { enabled ->
                                scope.launch {
                                    runCatching {
                                        SupabaseProvider.client.from("daily_scriptures").update(mapOf("is_active" to enabled)) { filter { eq("id", entry.id) } }
                                        reload()
                                    }.onFailure { error = it.message ?: "Unable to update Scripture entry." }
                                }
                            })
                            IconButton(onClick = { entryPendingDelete = entry }) {
                                Icon(Icons.Default.Delete, "Delete Scripture")
                            }
                        }
                    }
                }
            }
        }
    }

    themePendingDelete?.let { theme ->
        AlertDialog(
            onDismissRequest = { themePendingDelete = null },
            title = { Text("Delete theme?") },
            text = { Text("This will permanently remove the theme. Themes with Scripture entries cannot be deleted.") },
            confirmButton = {
                Button(onClick = {
                    themePendingDelete = null
                    scope.launch {
                        runCatching {
                            SupabaseProvider.client.from("daily_scripture_themes").delete { filter { eq("id", theme.id) } }
                            reload()
                        }.onFailure { error = it.message ?: "Unable to delete theme." }
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { themePendingDelete = null }) { Text("Cancel") } }
        )
    }

    entryPendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryPendingDelete = null },
            title = { Text("Delete Scripture entry?") },
            text = { Text("This removes the passage from the Today's Scripture library. This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    entryPendingDelete = null
                    scope.launch {
                        runCatching {
                            SupabaseProvider.client.from("daily_scriptures").delete { filter { eq("id", entry.id) } }
                            reload()
                        }.onFailure { error = it.message ?: "Unable to delete Scripture entry." }
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { entryPendingDelete = null }) { Text("Cancel") } }
        )
    }

    if (showThemeDialog) {
        ThemeDialog(
            onDismiss = { showThemeDialog = false },
            onSave = { name, description ->
                scope.launch {
                    runCatching {
                        val slug = name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
                        SupabaseProvider.client.from("daily_scripture_themes").insert(
                            mapOf("slug" to slug, "name" to name.trim(), "description" to description.trim(), "sort_order" to ((themes.maxOfOrNull { it.sort_order } ?: 0) + 10))
                        )
                        showThemeDialog = false
                        reload()
                    }.onFailure { error = it.message ?: "Unable to create theme." }
                }
            }
        )
    }

    if (showEntryDialog) {
        EntryDialog(
            themes = themes,
            books = books,
            translations = translations,
            onDismiss = { showEntryDialog = false },
            onSave = { theme, translation, book, chapter, start, end, situation, reflection ->
                scope.launch {
                    runCatching {
                        SupabaseProvider.client.from("daily_scriptures").insert(
                            mapOf(
                                "theme_id" to theme.id,
                                "translation_id" to translation.id,
                                "book_id" to book.id,
                                "chapter" to chapter,
                                "verse_start" to start,
                                "verse_end" to end,
                                "situation" to situation,
                                "reflection" to reflection,
                                "priority" to 10,
                                "is_active" to true
                            )
                        )
                        showEntryDialog = false
                        reload()
                    }.onFailure { error = it.message ?: "Unable to create Scripture entry." }
                }
            }
        )
    }
}

@Composable
private fun ThemeDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add theme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Theme name") })
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, minLines = 2)
            }
        },
        confirmButton = { Button(onClick = { onSave(name, description) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDialog(
    themes: List<ThemeRow>,
    books: List<BibleBook>,
    translations: List<BibleTranslation>,
    onDismiss: () -> Unit,
    onSave: (ThemeRow, BibleTranslation, BibleBook, Int, Int, Int, String, String) -> Unit
) {
    var theme by remember { mutableStateOf(themes.first()) }
    var translation by remember { mutableStateOf(translations.firstOrNull { it.id.equals("kjv", true) } ?: translations.first()) }
    var book by remember { mutableStateOf(books.first()) }
    var chapter by remember { mutableStateOf("1") }
    var start by remember { mutableStateOf("1") }
    var end by remember { mutableStateOf("1") }
    var situation by remember { mutableStateOf("") }
    var reflection by remember { mutableStateOf("") }
    var expandedTheme by remember { mutableStateOf(false) }
    var expandedBook by remember { mutableStateOf(false) }
    var expandedTranslation by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Scripture") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedTextField(
                        value = theme.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Theme") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = expandedTheme,
                        onDismissRequest = { expandedTheme = false }
                    ) {
                        themes.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name) },
                                onClick = { theme = item; expandedTheme = false }
                            )
                        }
                    }
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.matchParentSize().clickable { expandedTheme = true }
                    )
                }
                Box {
                    OutlinedTextField(
                        value = translation.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Translation") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = expandedTranslation,
                        onDismissRequest = { expandedTranslation = false }
                    ) {
                        translations.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name) },
                                onClick = { translation = item; expandedTranslation = false }
                            )
                        }
                    }
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.matchParentSize().clickable { expandedTranslation = true }
                    )
                }
                Box {
                    OutlinedTextField(
                        value = book.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Book") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = expandedBook,
                        onDismissRequest = { expandedBook = false }
                    ) {
                        books.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name) },
                                onClick = { book = item; expandedBook = false }
                            )
                        }
                    }
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.matchParentSize().clickable { expandedBook = true }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(chapter, { chapter = it.filter(Char::isDigit) }, label = { Text("Chapter") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(start, { start = it.filter(Char::isDigit) }, label = { Text("Verse") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(end, { end = it.filter(Char::isDigit) }, label = { Text("To") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(situation, { situation = it }, label = { Text("Life situation") }, minLines = 2)
                OutlinedTextField(reflection, { reflection = it }, label = { Text("Reflection") }, minLines = 2)
            }
        },
        confirmButton = {
            val c = chapter.toIntOrNull()
            val s = start.toIntOrNull()
            val e = end.toIntOrNull()
            Button(
                onClick = { onSave(theme, translation, book, c!!, s!!, e!!, situation, reflection) },
                enabled = c != null && s != null && e != null && c > 0 && s > 0 && e >= s
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
