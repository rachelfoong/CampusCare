package com.university.campuscare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.university.campuscare.ui.screens.tabs.AlertsTab
import com.university.campuscare.ui.screens.tabs.HomeTab
import com.university.campuscare.ui.screens.tabs.IssuesTab
import com.university.campuscare.ui.screens.tabs.ProfileTab
import com.university.campuscare.viewmodel.AuthViewModel

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    object Issues : BottomNavItem("issues", "Issues", Icons.AutoMirrored.Filled.List)
    object Alerts : BottomNavItem("alerts", "Alerts", Icons.Default.Notifications)
    object Profile : BottomNavItem("profile", "Profile", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToReportFault: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHelpSupport: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToIssueDetails: (String) -> Unit,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Issues,
        BottomNavItem.Alerts,
        BottomNavItem.Profile
    )

    val authState by authViewModel.authState.collectAsState()
    val userName = if (authState is com.university.campuscare.viewmodel.AuthState.Authenticated) {
        (authState as com.university.campuscare.viewmodel.AuthState.Authenticated).user.name
    } else {
        "User"
    }
    val userId = if (authState is com.university.campuscare.viewmodel.AuthState.Authenticated) {
        (authState as com.university.campuscare.viewmodel.AuthState.Authenticated).user.userId
    } else {
        ""
    }


    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFF0000),
                            selectedTextColor = Color(0xFFFF0000),
                            indicatorColor = Color(0xFFFFEBEB)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> HomeTab(userName, userId, onNavigateToReportFault)
                1 -> IssuesTab(userId, onNavigateToChat, onNavigateToIssueDetails)
                2 -> AlertsTab(userId)
                3 -> ProfileTab(userName, onLogout, onNavigateToSettings, onNavigateToHelpSupport)
            }
        }
    }
}
