package com.university.campuscare.data.model

data class Notification(
    val id: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.STATUS_UPDATE,
    val title: String = "",
    val message: String = "",
    val issueId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

enum class NotificationType {
    ISSUE_RESOLVED,
    STATUS_UPDATE,
    NEW_MESSAGE,
    MAINTENANCE_SCHEDULE
}
