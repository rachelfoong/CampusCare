package com.university.campuscare.data.model

import com.google.firebase.firestore.PropertyName

data class Issue(
    @get:PropertyName("id") @field:PropertyName("id") val id: String = "",
    @get:PropertyName("category") @field:PropertyName("category") val category: String = "",
    @get:PropertyName("title") @field:PropertyName("title") val title: String = "",
    @get:PropertyName("description") @field:PropertyName("description") val description: String = "",
    @get:PropertyName("location") @field:PropertyName("location") val location: IssueLocation = IssueLocation(),
    @get:PropertyName("status") @field:PropertyName("status") val status: IssueStatus = IssueStatus.PENDING,
    @get:PropertyName("urgency") @field:PropertyName("urgency") val urgency: IssueUrgency = IssueUrgency.MEDIUM,
    @get:PropertyName("reportedBy") @field:PropertyName("reportedBy") val reportedBy: String = "",
    @get:PropertyName("reporterName") @field:PropertyName("reporterName") val reporterName: String = "",
    @get:PropertyName("assignedTo") @field:PropertyName("assignedTo") val assignedTo: String? = null,
    @get:PropertyName("assignedToName") @field:PropertyName("assignedToName") val assignedToName: String? = null,
    @get:PropertyName("photoUrl") @field:PropertyName("photoUrl") val photoUrl: String? = null,
    @get:PropertyName("createdAt") @field:PropertyName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @get:PropertyName("updatedAt") @field:PropertyName("updatedAt") val updatedAt: Long = System.currentTimeMillis()
)

data class IssueLocation(
    @get:PropertyName("block") @field:PropertyName("block") val block: String = "",
    @get:PropertyName("level") @field:PropertyName("level") val level: String = "",
    @get:PropertyName("room") @field:PropertyName("room") val room: String = "",
    @get:PropertyName("address") @field:PropertyName("address") val address: String? = null,
    @get:PropertyName("latitude") @field:PropertyName("latitude") val latitude: Double? = null,
    @get:PropertyName("longitude") @field:PropertyName("longitude") val longitude: Double? = null
)

enum class IssueStatus {
    PENDING,
    IN_PROGRESS,
    RESOLVED
}

enum class IssueUrgency {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class IssueCategory {
    LIFT,
    TOILET,
    WIFI,
    CLASSROOM,
    OTHER
}
