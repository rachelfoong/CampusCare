package com.university.campuscare.data.model

import com.google.firebase.firestore.PropertyName

data class Notification(
    @get:PropertyName("id") @field:PropertyName("id") val id: String = "",
    @get:PropertyName("userId") @field:PropertyName("userId") val userId: String = "",
    @get:PropertyName("type") @field:PropertyName("type") val type: NotificationType = NotificationType.STATUS_UPDATE,
    @get:PropertyName("title") @field:PropertyName("title") val title: String = "",
    @get:PropertyName("message") @field:PropertyName("message") val message: String = "",
    @get:PropertyName("issueId") @field:PropertyName("issueId") val issueId: String? = null,
    @get:PropertyName("timestamp") @field:PropertyName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("read") @field:PropertyName("read") val read: Boolean = false
)

enum class NotificationType {
    ISSUE_RESOLVED,
    STATUS_UPDATE,
    NEW_MESSAGE,
    MAINTENANCE_SCHEDULE
}
