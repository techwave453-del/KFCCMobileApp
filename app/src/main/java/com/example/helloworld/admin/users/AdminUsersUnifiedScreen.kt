package com.example.helloworld.admin.users

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloworld.admin.AdminUser
import com.example.helloworld.admin.members.CommunityMembersScreen

@Composable
fun AdminUsersUnifiedScreen(
    modifier: Modifier = Modifier,
    currentUser: AdminUser,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Community Members", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Group, null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Administrators & Requests", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.AdminPanelSettings, null) }
            )
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (selectedTab) {
                0 -> CommunityMembersScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel(),
                    currentUser = currentUser
                )
                1 -> AdminUsersScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel(factory = AdminUsersViewModel.Factory(application)),
                    currentUser = currentUser,
                    onBack = onBack
                )
            }
        }
    }
}
