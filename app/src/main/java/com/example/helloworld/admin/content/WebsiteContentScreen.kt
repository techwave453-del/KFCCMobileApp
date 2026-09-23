package com.example.helloworld.admin.content

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun WebsiteContentScreen(
    modifier: Modifier = Modifier,
    viewModel: WebsiteContentViewModel = viewModel(
        factory = WebsiteContentViewModel.Factory(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val pages by viewModel.pages.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val selectedPageId by viewModel.selectedPageId.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val error by viewModel.error.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val selectedPage = pages.firstOrNull { it.id == selectedPageId }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Website Content", style = MaterialTheme.typography.headlineSmall)
                Text("Edit CMS pages and sections directly in Supabase.", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = viewModel::refresh, enabled = !loading && !saving) {
                Icon(Icons.Default.Refresh, "Refresh CMS")
            }
        }
        Text("Changes are protected by Supabase RLS and your administrator permissions.", style = MaterialTheme.typography.bodySmall)

        if (pages.isEmpty() && loading) {
            CircularProgressIndicator()
        } else if (pages.isEmpty()) {
            Text("No CMS pages are available.")
        } else {
            Text("Pages", style = MaterialTheme.typography.titleMedium)
            pages.forEach { page ->
                OutlinedButton(
                    onClick = { viewModel.selectPage(page) },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(page.menu_label.ifBlank { page.internal_name } + "  •  " + page.status)
                }
            }

            selectedPage?.let { page ->
                HorizontalDivider()
                Text(page.menu_label.ifBlank { page.internal_name }, style = MaterialTheme.typography.titleLarge)
                Text("Slug: /" + page.slug, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("draft", "published", "archived").forEach { status ->
                        OutlinedButton(
                            onClick = { viewModel.setPageStatus(page, status) },
                            enabled = !saving && page.status != status,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(status.replaceFirstChar { it.uppercase() })
                        }
                    }
                }
                sections.forEach { section ->
                    CmsSectionEditor(section, saving) { heading, body, media, eyebrow ->
                        viewModel.saveSection(section, heading, body, media, eyebrow)
                    }
                }
            }
        }

        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        if (saved) Text("CMS changes saved successfully.", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CmsSectionEditor(
    section: CmsSection,
    saving: Boolean,
    onSave: (String, String, String, String) -> Unit
) {
    var heading by remember(section.id, section.content) {
        mutableStateOf(section.content["heading"]?.jsonPrimitive?.contentOrNull.orEmpty())
    }
    var body by remember(section.id, section.content) {
        mutableStateOf(section.content["body"]?.jsonPrimitive?.contentOrNull.orEmpty())
    }
    var media by remember(section.id, section.content) {
        mutableStateOf(section.content["media"]?.jsonPrimitive?.contentOrNull.orEmpty())
    }
    var eyebrow by remember(section.id, section.content) {
        mutableStateOf(section.content["eyebrow"]?.jsonPrimitive?.contentOrNull.orEmpty())
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(section.section_type.replaceFirstChar { it.uppercase() } + " • Section " + (section.position + 1), style = MaterialTheme.typography.titleMedium)
            when (section.section_type.lowercase()) {
                "hero" -> {
                    OutlinedTextField(eyebrow, { eyebrow = it }, label = { Text("Eyebrow") }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(heading, { heading = it }, label = { Text("Hero Heading") }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(body, { body = it }, label = { Text("Hero Text") }, minLines = 3, enabled = !saving, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(media, { media = it }, label = { Text("Hero Image URL") }, enabled = !saving, modifier = Modifier.fillMaxWidth())
                }
                "youtube", "video" -> {
                    OutlinedTextField(heading, { heading = it }, label = { Text("Video Title") }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(media, { media = it }, label = { Text("YouTube / Video URL") }, enabled = !saving, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(body, { body = it }, label = { Text("Description") }, minLines = 3, enabled = !saving, modifier = Modifier.fillMaxWidth())
                }
                "giving" -> {
                    OutlinedTextField(heading, { heading = it }, label = { Text("Giving Heading") }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(body, { body = it }, label = { Text("Giving Message") }, minLines = 3, enabled = !saving, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(media, { media = it }, label = { Text("Giving Link / URL") }, enabled = !saving, modifier = Modifier.fillMaxWidth())
                }
                "contact" -> {
                    OutlinedTextField(heading, { heading = it }, label = { Text("Contact Heading") }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(body, { body = it }, label = { Text("Contact Details") }, minLines = 4, enabled = !saving, modifier = Modifier.fillMaxWidth())
                }
                "image_text", "gallery", "cards", "cta" -> {
                    OutlinedTextField(heading, { heading = it }, label = { Text("Heading") }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(body, { body = it }, label = { Text("Text") }, minLines = 3, enabled = !saving, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(media, { media = it }, label = { Text("Image / Media URL") }, enabled = !saving, modifier = Modifier.fillMaxWidth())
                }
                else -> {
                    OutlinedTextField(heading, { heading = it }, label = { Text("Heading") }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(body, { body = it }, label = { Text("Body") }, minLines = 3, enabled = !saving, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(media, { media = it }, label = { Text("Media / URL") }, enabled = !saving, modifier = Modifier.fillMaxWidth())
                }
            }

            Button(onClick = { onSave(heading, body, media, eyebrow) }, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                if (saving) CircularProgressIndicator(Modifier.size(20.dp)) else Text("Save Section")
            }
        }
    }
}
