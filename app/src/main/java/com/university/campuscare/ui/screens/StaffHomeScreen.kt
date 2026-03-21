package com.university.campuscare.ui.screens

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
import com.university.campuscare.ui.screens.tabs.IssuesTab
import com.university.campuscare.ui.screens.tabs.ProfileTab
import com.university.campuscare.viewmodel.AuthState
import com.university.campuscare.viewmodel.AuthViewModel

sealed class StaffBottomNavItem(val title: String, val icon: ImageVector) {
    object Tasks : StaffBottomNavItem("Tasks", Icons.AutoMirrored.Filled.Assignment)
    object Alerts : StaffBottomNavItem("Alerts", Icons.Default.Notifications)
    object Profile : StaffBottomNavItem("Profile", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffHomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHelpSupport: () -> Unit,
    onNavigateToUserProfile: () -> Unit,
    onNavigateToFacilitiesTeam: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToIssueDetails: (String) -> Unit,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val bottomNavItems = listOf(
        StaffBottomNavItem.Tasks,
        StaffBottomNavItem.Alerts,
        StaffBottomNavItem.Profile
    )

    val authState by authViewModel.authState.collectAsState()
    val userName = if (authState is AuthState.Authenticated) (authState as AuthState.Authenticated).user.name else "Staff"
    val userId = if (authState is AuthState.Authenticated) (authState as AuthState.Authenticated).user.userId else ""
    val userRole = "STAFF" // Hardcoded since only staff arrive here

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (selectedTab) {
                // Passes "STAFF" role down so IssuesTab searches the "assignedTo" field
                0 -> IssuesTab(userId, userRole, onNavigateToChat, onNavigateToIssueDetails)
                1 -> AlertsTab(userId)
                2 -> ProfileTab(userName, onLogout, onNavigateToSettings, onNavigateToHelpSupport, onNavigateToFacilitiesTeam, onNavigateToUserProfile)
            }
        }
    }
}