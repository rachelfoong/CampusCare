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
    object UserProfile : Screen("user_profile")
    object Chat : Screen("chat/{issueId}?issueTitle={issueTitle}") {
        fun createRoute(issueId: String, issueTitle: String = "Chat"): String {
            return "chat/$issueId?issueTitle=$issueTitle"
        }
    }
    object DirectChat : Screen("direct_chat/{adminId}?adminName={adminName}") {
        fun createRoute(adminId: String, adminName: String = "Admin"): String {
            return "direct_chat/$adminId?adminName=$adminName"
        }
    }
    object IssueDetail : Screen("issue_detail/{issueId}") {
        fun createRoute(issueId: String) = "issue_detail/$issueId"
    }
    object FacilitiesTeam : Screen("facilities_team")
    object ProfileSearch : Screen("profile_search")
    object DetailedProfile : Screen("detailed_profile/{userId}") {
        fun createRoute(userId: String) = "detailed_profile/$userId"
    }
}