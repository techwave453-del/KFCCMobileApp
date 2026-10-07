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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.helloworld.admin.AdminRepositoryProvider
import com.example.helloworld.admin.AppUpdateConfig

@Composable
fun AppUpdateAdminScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val repository = remember(application) { AdminRepositoryProvider.get(application) }
    val scope = rememberCoroutineScope()

    var config by remember { mutableStateOf<AppUpdateConfig?>(null) }
    var downloadUrl by remember { mutableStateOf("") }
    var releaseNotes by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repository.getAppUpdateConfig().onSuccess { loaded ->
            config = loaded
            downloadUrl = loaded.downloadUrl
            releaseNotes = loaded.releaseNotes
            enabled = loaded.isEnabled
        }.onFailure {
            message = it.message ?: "Unable to load app update configuration."
        }
        loading = false
    }

    val latestBuildCode = config?.latestBuildVersionCode
    val latestBuildName = config?.latestBuildVersionName

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
                Text("Configure which successful Android build is published to members.")
            }
        }

        if (loading) {
            CircularProgressIndicator()
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Latest successful build", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (latestBuildCode == null || latestBuildName.isNullOrBlank()) {
                        Text("No successful release build has been recorded yet.")
                        Text("Run the release-build workflow after creating a new production version.")
                    } else {
                        Text("$latestBuildName (version code $latestBuildCode)")
                        config?.latestBuildAt?.let { Text("Built: $it") }
                        config?.latestBuildCommit?.let { Text("Commit: $it") }
                        if (latestBuildCode > (config?.versionCode ?: 0)) {
                            Text(
                                "Ready to publish. The APK URL below will be used for this build.",
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                "This build is already published or is not newer than the published version.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Currently published update", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${config?.versionName ?: "—"} (version code ${config?.versionCode ?: "—"})")
                    Text(if (config?.isEnabled == true) "Automatic updates are enabled." else "Automatic updates are disabled.")
                    Text("The published version is what member devices will compare against.")
                }
            }

            OutlinedTextField(
                value = downloadUrl,
                onValueChange = { downloadUrl = it },
                label = { Text("APK download URL") },
                supportingText = {
                    Text("Enter the APK URL for the latest successful build. Google Drive shared links are supported.")
                },
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
                    if (latestBuildCode == null || latestBuildName.isNullOrBlank()) {
                        message = "There is no successful release build to publish yet."
                        return@Button
                    }
                    if (latestBuildCode <= (config?.versionCode ?: 0)) {
                        message = "The latest successful build is not newer than the currently published version."
                        return@Button
                    }
                    if (downloadUrl.isBlank()) {
                        message = "Enter the APK download URL for this build."
                        return@Button
                    }

                    saving = true
                    message = null
                    scope.launch {
                        repository.publishLatestSuccessfulBuild(
                            downloadUrl = downloadUrl.trim(),
                            releaseNotes = releaseNotes.trim(),
                            isEnabled = enabled
                        ).onSuccess {
                            message = "Version $latestBuildName has been published."
                            repository.getAppUpdateConfig().onSuccess { config = it }
                        }.onFailure {
                            message = it.message ?: "Unable to publish the update."
                        }
                        saving = false
                    }
                },
                enabled = !saving && latestBuildCode != null && latestBuildCode > (config?.versionCode ?: 0),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("Publish Latest Successful Build")
            }

            config?.let {
                Text(
                    "Current release URL: ${it.downloadUrl.ifBlank { "Not configured" }}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            message?.let {
                Text(
                    it,
                    color = if (
                        it.contains("published", ignoreCase = true) ||
                        it.contains("saved", ignoreCase = true)
                    ) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Release process: build a production APK → the successful build is recorded automatically → upload that APK to your chosen host → paste its URL here → publish it. Debug builds never change the public update version.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
