package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.ChurchService
import com.example.helloworld.ui.components.ChurchServiceCard

@Composable
fun ServicesScreen(
    services: List<ChurchService>,
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Services", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text("Join us in worship, prayer and fellowship.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }
        items(services) { service ->
            ChurchServiceCard(service = service)
        }
    }
}
