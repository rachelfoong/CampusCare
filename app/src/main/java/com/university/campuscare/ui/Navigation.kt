package com.university.campuscare.ui

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Home : Screen("home")
    object AdminHome : Screen("admin_home")
    object ReportFault : Screen("report_fault")
    object Settings : Screen("settings")
    object HelpSupport : Screen("help_support")
}