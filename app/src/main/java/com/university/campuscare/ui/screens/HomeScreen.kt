package com.university.campuscare.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
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
import com.university.campuscare.viewmodel.AuthState
import com.university.campuscare.viewmodel.AuthViewModel

sealed class BottomNavItem(
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem("Home", Icons.Default.Home)
    object Issues : BottomNavItem("Issues", Icons.AutoMirrored.Filled.Assignment)
    object Alerts : BottomNavItem("Alerts", Icons.Default.Notifications)
    object Profile : BottomNavItem("Profile", Icons.Default.Person)
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToReportFault: (String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHelpSupport: () -> Unit,
    onNavigateToUserProfile: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToIssueDetails: (String) -> Unit,
    onNavigateToFacilitiesTeam: () -> Unit,
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
    val userName = if (authState is AuthState.Authenticated) (authState as AuthState.Authenticated).user.name else "User"
    val userId = if (authState is AuthState.Authenticated) (authState as AuthState.Authenticated).user.userId else ""
    val userRole = "STUDENT" // Hardcoded since only student arrive here


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
                1 -> IssuesTab(userId, userRole, onNavigateToChat, onNavigateToIssueDetails)
                2 -> AlertsTab(userId)
                3 -> ProfileTab(userName, onLogout, onNavigateToSettings, onNavigateToHelpSupport, onNavigateToFacilitiesTeam, onNavigateToUserProfile)
            }
        }
    }
}
