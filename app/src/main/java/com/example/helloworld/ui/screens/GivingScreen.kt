package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GivingScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.VolunteerActivism, null, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Giving", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Support the ministry and help us continue serving the church and community.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = { }) { Text("Giving options coming next") }
    }
}
