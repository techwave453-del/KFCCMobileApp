package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.ChurchInfo
import com.example.helloworld.ui.ChurchViewModel

@Composable
fun AdminDashboard(
    viewModel: ChurchViewModel,
    onNavigateToUpload: () -> Unit,
    onLogout: () -> Unit,
    innerPadding: PaddingValues
) {
    val churchInfo by viewModel.churchInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scrollState = rememberScrollState()
    
    // Local state for editing
    var liveEnabled by remember(churchInfo) { mutableStateOf(churchInfo.liveStream.enabled) }
    var liveUrl by remember(churchInfo) { mutableStateOf(churchInfo.liveStream.url) }
    var liveTitle by remember(churchInfo) { mutableStateOf(churchInfo.liveStream.title) }
    var liveDesc by remember(churchInfo) { mutableStateOf(churchInfo.liveStream.description) }
    var phone by remember(churchInfo) { mutableStateOf(churchInfo.phone) }
    var email by remember(churchInfo) { mutableStateOf(churchInfo.email) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Admin Dashboard", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = {
                viewModel.logout()
                onLogout()
            }) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Live Stream Settings", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = liveEnabled, onCheckedChange = { liveEnabled = it })
                    Text(text = "Enable Live Stream")
                }
                
                OutlinedTextField(
                    value = liveUrl,
                    onValueChange = { liveUrl = it },
                    label = { Text("YouTube Live URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = liveTitle,
                    onValueChange = { liveTitle = it },
                    label = { Text("Live Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = liveDesc,
                    onValueChange = { liveDesc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Contact Information", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                val updated = churchInfo.copy(
                    liveStream = churchInfo.liveStream.copy(
                        enabled = liveEnabled,
                        url = liveUrl,
                        title = liveTitle,
                        description = liveDesc
                    ),
                    phone = phone,
                    email = email
                )
                viewModel.updateSiteContent(updated) { success ->
                    // Handle success/failure toast
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Save Changes")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onNavigateToUpload,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Upload New Media")
        }
    }
}
