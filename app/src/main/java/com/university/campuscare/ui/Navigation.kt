package com.university.campuscare.ui

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Home : Screen("home")
    object AdminHome : Screen("admin_home")
    object ReportFault : Screen("report_fault?category={category}") {
        fun createRoute(category: String? = null): String {
            return if (category != null) "report_fault?category=$category" else "report_fault"
        }
    }
    object Settings : Screen("settings")
    object HelpSupport : Screen("help_support")
    object Chat : Screen("chat/{issueId}/{issueTitle}") {
        fun createRoute(issueId: String, issueTitle: String) = "chat/$issueId/$issueTitle"
    }
    object IssueDetail : Screen("issue_detail/{issueId}") {
        fun createRoute(issueId: String) = "issue_detail/$issueId"
    }
}