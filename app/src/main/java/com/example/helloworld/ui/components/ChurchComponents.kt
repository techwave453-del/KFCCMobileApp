package com.example.helloworld.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.helloworld.data.ChurchLink
import com.example.helloworld.data.ChurchService
import com.example.helloworld.data.MembershipClass

@Composable
fun ChurchHero(title: String, subtitle: String, modifier: Modifier = Modifier, imageUrl: String? = null, churchName: String = "Kingdom Fellowship Christian Church", onPrimaryAction: () -> Unit = {}) {
    val fallbackImage = "https://images.unsplash.com/photo-1519491050282-cf00c82424b4?auto=format&fit=crop&w=1600&q=85"
    Box(modifier.fillMaxWidth().height(390.dp).clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))) {
        AsyncImage(model = imageUrl?.takeIf { it.isNotBlank() } ?: fallbackImage, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.18f), Color.Black.copy(.42f), Color.Black.copy(.88f)))))
        Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.Bottom) {
            Surface(color = MaterialTheme.colorScheme.primary.copy(.92f), shape = RoundedCornerShape(50.dp)) {
                Text(churchName.uppercase(), Modifier.padding(horizontal = 13.dp, vertical = 7.dp), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 42.sp)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(.92f), maxLines = 3)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onPrimaryAction, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
                Text("▶  Watch & Worship", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        if (action != null) TextButton(onClick = onAction) { Text("$action  →", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun ChurchServiceCard(service: ChurchService, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column {
            Box {
                AsyncImage(model = service.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(155.dp).clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)), contentScale = ContentScale.Crop)
                Surface(color = Color.Black.copy(.62f), shape = RoundedCornerShape(50.dp), modifier = Modifier.padding(12.dp)) {
                    Text("▶  WATCH", color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.padding(16.dp)) {
                Text(service.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))
                Text(service.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ChurchLinkCard(link: ChurchLink, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = link.imageUrl, contentDescription = null, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text(link.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold); Text(link.text, style = MaterialTheme.typography.bodySmall, maxLines = 2) }
            Text("→", color = MaterialTheme.colorScheme.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MembershipClassCard(membershipClass: MembershipClass, modifier: Modifier = Modifier) {
    Card(modifier.width(200.dp), shape = RoundedCornerShape(18.dp)) {
        Box {
            AsyncImage(model = membershipClass.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(125.dp), contentScale = ContentScale.Crop)
            Surface(color = Color.Black.copy(.58f), modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()) { Text(membershipClass.title, color = Color.White, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
        }
    }
}
