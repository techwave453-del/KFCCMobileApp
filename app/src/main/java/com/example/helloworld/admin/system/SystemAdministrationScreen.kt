package com.example.helloworld.admin.system

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
private data class AuditRow(
    val id: Long,
    val user_id: Long? = null,
    val action: String,
    val resource: String? = null,
    val resource_id: String? = null,
    val details: JsonElement? = null,
    val created_at: String
)

/**
 * System Administration is intentionally read-only for the mobile client.
 * Security-sensitive account changes remain behind the Supabase admin-management
 * Edge Function, while audit data is read directly from Postgres through RLS.
 */
@Composable
fun SystemAdministrationScreen(
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<AuditRow>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            error = null
            runCatching {
                SupabaseProvider.client
                    .from("admin_audit_log")
                    .select()
                    .decodeList<AuditRow>()
                    .sortedByDescending { it.created_at }
            }.onSuccess { rows = it }
                .onFailure { error = it.message ?: "Unable to load the audit log." }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("System Administration", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Security audit activity read directly from Supabase.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = ::refresh, enabled = !loading) {
                Icon(Icons.Default.Refresh, "Refresh audit log")
            }
        }

        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        if (loading && rows.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No audit entries recorded yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rows, key = { it.id }) { row ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(row.action, style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOfNotNull(row.resource, row.resource_id?.let { "#$it" })
                                    .joinToString(" · ")
                                    .ifBlank { "System action" },
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(row.created_at, style = MaterialTheme.typography.labelSmall)
                            if (!row.details.isNullOrBlank()) {
                                Text(row.details!!, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
