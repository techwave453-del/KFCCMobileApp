package com.example.helloworld.admin.updates

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.helloworld.BuildConfig
import com.example.helloworld.admin.AdminRepositoryProvider
import com.example.helloworld.admin.AppUpdateConfig
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppUpdateAdminScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val repository = remember(application) { AdminRepositoryProvider.get(application) }

    var versionCode by remember { mutableStateOf("") }
    var versionName by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }
    var releaseNotes by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repository.getAppUpdateConfig().onSuccess { config ->
            versionCode = config.versionCode.toString()
            versionName = config.versionName
            downloadUrl = config.downloadUrl
            releaseNotes = config.releaseNotes
            enabled = config.isEnabled
        }.onFailure {
            message = it.message ?: "Unable to load app update configuration."
        }
        loading = false
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.SystemUpdate, contentDescription = null)
            Column {
                Text("App Updates", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Current installed version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            }
        }

        Text(
            "Set the APK version and download URL here. Users do not need to update the app code when you publish a new release.",
            style = MaterialTheme.typography.bodyMedium
        )

        if (loading) {
            CircularProgressIndicator()
        } else {
            OutlinedTextField(
                value = versionCode,
                onValueChange = { versionCode = it.filter(Char::isDigit) },
                label = { Text("Release version code") },
                supportingText = { Text("Must be higher than the installed version code.") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = versionName,
                onValueChange = { versionName = it },
                label = { Text("Release version name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = downloadUrl,
                onValueChange = { downloadUrl = it },
                label = { Text("APK download URL") },
                supportingText = { Text("Google Drive shared links are supported when the APK is downloadable by anyone with the link.") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            OutlinedTextField(
                value = releaseNotes,
                onValueChange = { releaseNotes = it },
                label = { Text("Release notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Switch(checked = enabled, onCheckedChange = { enabled = it })
                Text(
                    if (enabled) "Automatic updates enabled" else "Automatic updates disabled",
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Button(
                onClick = {
                    val code = versionCode.toIntOrNull()
                    if (code == null || code <= 0) {
                        message = "Enter a valid version code."
                        return@Button
                    }
                    if (code <= BuildConfig.VERSION_CODE) {
                        message = "The release version code must be greater than ${BuildConfig.VERSION_CODE}."
                        return@Button
                    }
                    if (downloadUrl.isBlank()) {
                        message = "Enter the APK download URL."
                        return@Button
                    }

                    saving = true
                    message = null
                    repository.saveAppUpdateConfig(
                        AppUpdateConfig(
                            versionCode = code,
                            versionName = versionName.trim(),
                            downloadUrl = downloadUrl.trim(),
                            releaseNotes = releaseNotes.trim(),
                            isEnabled = enabled
                        )
                    ).onSuccess {
                        message = "App update configuration saved."
                    }.onFailure {
                        message = it.message ?: "Unable to save app update configuration."
                    }
                    saving = false
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("Save Update Configuration")
            }

            message?.let {
                Text(it, color = if (it.contains("saved", ignoreCase = true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        }
    }
}
