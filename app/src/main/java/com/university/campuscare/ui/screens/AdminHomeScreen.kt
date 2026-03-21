package com.university.campuscare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.university.campuscare.viewmodel.AdminViewModel
import com.university.campuscare.viewmodel.AuthViewModel
import com.university.campuscare.ui.screens.tabs.*
import com.university.campuscare.viewmodel.AuthState

sealed class AdminBottomNavItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Dashboard : AdminBottomNavItem("Dashboard", Icons.Default.Dashboard)
    object Analytics : AdminBottomNavItem("Analytics", Icons.Default.BarChart)
    object Users : AdminBottomNavItem("Users", Icons.Default.Group)
    object StaffMgmt : AdminBottomNavItem("Staff", Icons.Default.Engineering)
    object Settings : AdminBottomNavItem("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    navController: NavController,
    onLogout: () -> Unit,
    onNavigateToChat: (String, String) -> Unit = { _, _ -> },
    authViewModel: AuthViewModel,
    viewModel: AdminViewModel = viewModel(),
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val bottomNavItems = listOf(
        AdminBottomNavItem.Dashboard,
        AdminBottomNavItem.Analytics,
        AdminBottomNavItem.Users,
        AdminBottomNavItem.StaffMgmt,
        AdminBottomNavItem.Settings
    )

    val authState by authViewModel.authState.collectAsState()
    val userName = if (authState is AuthState.Authenticated) (authState as AuthState.Authenticated).user.name else "Admin"

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                item.icon, 
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp)
                            ) 
                        },
                        label = { Text(item.title, fontSize = 11.sp) },
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
                0 -> AdminDashboardScreen(navController, userName, onNavigateToChat, viewModel)
                1 -> AdminAnalyticsTab(viewModel)
                2 -> AdminUsersTab(viewModel, authViewModel)
                3 -> StaffManagementTab( viewModel, authViewModel)
                4 -> AdminSettingsTab(userName, onLogout, navController)
            }
        }
    }
}