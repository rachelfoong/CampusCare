package com.university.campuscare.data.model

data class Issue(
    val id: String = "",
    val category: String = "",
    val title: String = "",
    val description: String = "",
    val location: IssueLocation = IssueLocation(),
    val status: IssueStatus = IssueStatus.PENDING,
    val reportedBy: String = "",
    val reporterName: String = "",
    val assignedTo: String? = null,
    val assignedToName: String? = null,
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class IssueLocation(
    val block: String = "",
    val level: String = "",
    val room: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)

enum class IssueStatus {
    PENDING,
    IN_PROGRESS,
    RESOLVED
}

enum class IssueCategory {
    LIFT,
    TOILET,
    WIFI,
    CLASSROOM,
    OTHER
}
