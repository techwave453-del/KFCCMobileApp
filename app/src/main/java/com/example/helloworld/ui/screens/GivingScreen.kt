package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.ChurchInfo

@Composable
fun GivingScreen(info: ChurchInfo, innerPadding: PaddingValues) {
    val uriHandler = LocalUriHandler.current
    
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Favorite, 
                contentDescription = null, 
                modifier = Modifier.size(100.dp), 
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Tithes & Offerings", 
                style = MaterialTheme.typography.headlineMedium, 
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Your generosity helps us continue our mission to reveal Christ to nations.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(40.dp))
            
            Button(
                onClick = { 
                    if (info.givingUrl.isNotBlank()) {
                        uriHandler.openUri(info.givingUrl)
                    }
                }, 
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = info.givingUrl.isNotBlank()
            ) {
                Text(
                    if (info.givingUrl.isNotBlank()) "Give Securely Online" else "Giving Link Not Set",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (info.givingUrl.isBlank()) {
                Text(
                    "Please contact the church office for giving instructions.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
