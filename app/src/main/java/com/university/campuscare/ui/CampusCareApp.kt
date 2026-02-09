package com.university.campuscare.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.university.campuscare.ui.screens.*
import com.university.campuscare.viewmodel.AuthViewModel
import com.university.campuscare.viewmodel.AuthState
import androidx.compose.runtime.collectAsState

@Composable
fun CampusCareApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                authViewModel = authViewModel,
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToAdminHome = {
                    navController.navigate(Screen.AdminHome.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToAdminHome = {
                    navController.navigate(Screen.AdminHome.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                authViewModel = authViewModel
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                authViewModel = authViewModel
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToReportFault = { category ->
                    navController.navigate(Screen.ReportFault.createRoute(category))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToHelpSupport = {
                    navController.navigate(Screen.HelpSupport.route)
                },
                onNavigateToChat = { issueId, issueTitle ->
                    navController.navigate(Screen.Chat.createRoute(issueId, issueTitle))
                },
                onNavigateToDirectChat = { adminId, adminName ->
                    navController.navigate(Screen.DirectChat.createRoute(adminId, adminName))
                },
                onNavigateToIssueDetails = { issueId ->
                    navController.navigate(Screen.IssueDetail.createRoute(issueId))
                },
                onNavigateToFacilitiesTeam = {
                    navController.navigate(Screen.FacilitiesTeam.route)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable(Screen.AdminHome.route) {
            AdminHomeScreen(
                navController = navController,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToChat = { issueId, issueTitle ->
                    navController.navigate(
                        Screen.Chat.createRoute(issueId, issueTitle)
                    )
                },
                authViewModel = authViewModel
            )
        }

        composable(
            route = Screen.ReportFault.route,
            arguments = listOf(
                navArgument("category") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val authState = authViewModel.authState.collectAsState().value
            val userId = if (authState is AuthState.Authenticated) authState.user.userId else ""
            val userName = if (authState is AuthState.Authenticated) authState.user.name else ""
            val category = backStackEntry.arguments?.getString("category")

            ReportFaultScreen(
                onNavigateBack = { navController.popBackStack() },
                userId = userId,
                userName = userName,
                initialCategory = category
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHelpSupport = { navController.navigate(Screen.HelpSupport.route) },
                onNavigateToUserProfile = { navController.navigate(Screen.UserProfile.route) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable(Screen.UserProfile.route) {
            val authState = authViewModel.authState.collectAsState().value
            val userId = if (authState is AuthState.Authenticated) authState.user.userId else ""
            UserProfileScreen(
                userId = userId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.HelpSupport.route) {
            HelpSupportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("issueId") { type = NavType.StringType },
                navArgument("issueTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val issueId = backStackEntry.arguments?.getString("issueId") ?: ""
            val issueTitle = backStackEntry.arguments?.getString("issueTitle") ?: ""
            val authState = authViewModel.authState.collectAsState().value
            val currentUserId = if (authState is AuthState.Authenticated) authState.user.userId else ""
            val currentUserName = if (authState is AuthState.Authenticated) authState.user.name else ""
            val isAdmin = if (authState is AuthState.Authenticated) authState.user.role == "ADMIN" else false

            ChatScreen(
                issueId = issueId,
                issueTitle = issueTitle,
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                isAdmin = isAdmin,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.DirectChat.route,
            arguments = listOf(
                navArgument("adminId") { type = NavType.StringType },
                navArgument("adminName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val adminId = backStackEntry.arguments?.getString("adminId") ?: ""
            val adminName = backStackEntry.arguments?.getString("adminName") ?: ""
            val authState = authViewModel.authState.collectAsState().value
            val currentUserId = if (authState is AuthState.Authenticated) authState.user.userId else ""
            val currentUserName = if (authState is AuthState.Authenticated) authState.user.name else ""

            DirectChatScreen(
                adminId = adminId,
                adminName = adminName,
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.IssueDetail.route,
            arguments = listOf(navArgument("issueId") { type = NavType.StringType })
        ) { backStackEntry ->
            val issueId = backStackEntry.arguments?.getString("issueId") ?: ""
            val authState = authViewModel.authState.collectAsState().value
            val isAdmin = if (authState is AuthState.Authenticated) authState.user.role == "ADMIN" else false
            IssueDetailScreen(
                issueId = issueId,
                isAdmin = isAdmin,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { id, title ->
                    navController.navigate(Screen.Chat.createRoute(id, title))
                }
            )
        }

        composable(Screen.FacilitiesTeam.route) {
            FacilitiesTeamScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}