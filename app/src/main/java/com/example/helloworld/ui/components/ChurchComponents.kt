package com.example.helloworld.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.helloworld.data.ChurchLink
import com.example.helloworld.data.ChurchService
import com.example.helloworld.data.MembershipClass
import kotlinx.coroutines.delay

@Composable
fun ChurchHero(
    title: String,
    subtitle: String,
    backgroundImages: List<String> = emptyList(),
    onLiveClick: () -> Unit = {},
    isLive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { if (backgroundImages.isEmpty()) 1 else backgroundImages.size })

    if (backgroundImages.isNotEmpty()) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(5000)
                if (pagerState.pageCount > 0) {
                    val next = (pagerState.currentPage + 1) % pagerState.pageCount
                    pagerState.animateScrollToPage(next)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
    ) {
        // Background
        if (backgroundImages.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false
            ) { page ->
                AsyncImage(
                    model = backgroundImages[page],
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = CircleShape,
            ) {
                Text(
                    text = "WELCOME",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Jesus Changes Lives",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Italic
            )
        }

        // Live Button
        if (isLive) {
            SmallFloatingActionButton(
                onClick = onLiveClick,
                containerColor = Color(0xFFD32F2F),
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("LIVE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        if (actionText != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(actionText, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (imageUrl != null) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (imageUrl != null) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (imageUrl != null) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ModernEventCard(
    title: String,
    dateString: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val dateParts = try {
        val normalized = dateString.replace('T', ' ')
        val parts = normalized.split(' ')
        val datePart = parts.getOrNull(0) ?: ""
        val timePart = parts.getOrNull(1) ?: ""
        val dateSplit = datePart.split('-')
        val day = dateSplit.getOrNull(2) ?: "??"
        val monthNum = dateSplit.getOrNull(1)?.toIntOrNull() ?: 0
        val monthNames = listOf("", "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        val month = monthNames.getOrNull(monthNum) ?: "EVT"
        val timeSplit = timePart.split(':')
        val rawHour = timeSplit.getOrNull(0)?.toIntOrNull()
        val minute = timeSplit.getOrNull(1) ?: ""
        val time = if (rawHour != null && minute.isNotBlank()) {
            val ampm = if (rawHour >= 12) "PM" else "AM"
            val hour = if (rawHour % 12 == 0) 12 else rawHour % 12
            "$hour:$minute $ampm"
        } else {
            timePart
        }
        Triple(day, month, time)
    } catch (e: Exception) {
        Triple("??", "EVT", "")
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(60.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dateParts.first,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = dateParts.second,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (dateParts.third.isNotBlank()) {
                    Text(
                        text = dateParts.third,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ChurchServiceCard(
    service: ChurchService,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AsyncImage(
                model = service.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = service.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = service.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ChurchLinkCard(
    link: ChurchLink,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = link.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = link.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = link.text,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun MembershipClassCard(
    membershipClass: MembershipClass,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(200.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box {
            AsyncImage(
                model = membershipClass.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
            ) {
                Text(
                    text = membershipClass.title,
                    color = Color.White,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
