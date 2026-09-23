package com.example.helloworld.admin.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ChurchIdentityScreen(
    modifier: Modifier = Modifier,
    viewModel: IdentityViewModel = viewModel(factory = IdentityViewModel.Factory(androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application))
) {
    val identity by viewModel.identity.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val error by viewModel.error.collectAsState()
    val saved by viewModel.saved.collectAsState()

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Church Identity", style = MaterialTheme.typography.headlineSmall)
                Text("Highly protected official church information", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = viewModel::refresh, enabled = !loading && !saving) { Icon(Icons.Default.Refresh, "Refresh identity") }
        }
        Text("Only administrators with Church Identity permission can edit these fields. Server authorization remains the final authority.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(identity.churchName, { viewModel.update(identity.copy(churchName = it)) }, label = { Text("Church name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !saving)
        OutlinedTextField(identity.officialName, { viewModel.update(identity.copy(officialName = it)) }, label = { Text("Official identity / legal name") }, modifier = Modifier.fillMaxWidth(), enabled = !saving)
        OutlinedTextField(identity.registrationDetails, { viewModel.update(identity.copy(registrationDetails = it)) }, label = { Text("Registration / organization details") }, modifier = Modifier.fillMaxWidth(), enabled = !saving, minLines = 3)
        OutlinedTextField(identity.logoUrl, { viewModel.update(identity.copy(logoUrl = it)) }, label = { Text("Logo URL") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !saving)
        OutlinedTextField(identity.officialLogo, { viewModel.update(identity.copy(officialLogo = it)) }, label = { Text("Official logo URL") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !saving)
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        if (saved) Text("Church Identity saved successfully.", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Button(onClick = viewModel::save, enabled = !loading && !saving && identity.churchName.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            if (saving) CircularProgressIndicator(modifier = Modifier.height(20.dp)) else Text("Save Church Identity")
        }
    }
}
