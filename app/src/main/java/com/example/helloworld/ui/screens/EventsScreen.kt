package com.example.helloworld.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.helloworld.data.ChurchInfo
import com.example.helloworld.ui.components.ChurchLinkCard
import com.example.helloworld.ui.components.MembershipClassCard

@Composable
fun EventsScreen(info: ChurchInfo, innerPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Upcoming Programs",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(info.links.filter { it.url == "#events" || it.url == "#visit" }) { link ->
            ChurchLinkCard(
                link = link,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Faith & Membership Classes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(info.membershipClasses) { membershipClass ->
                    MembershipClassCard(membershipClass = membershipClass)
                }
            }
        }
    }
}
