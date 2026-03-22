package com.university.campuscare.data.model

import com.google.firebase.firestore.PropertyName

data class StaffHierarchyInsight(
    @get:PropertyName("staffId") @field:PropertyName("staffId") val staffId: String = "",
    @get:PropertyName("staffName") @field:PropertyName("staffName") val staffName: String = "",

    @get:PropertyName("issuesHandled") @field:PropertyName("issuesHandled") val issuesHandled: Long = 0L,

    @get:PropertyName("averageStaffHierarchyScore") @field:PropertyName("averageStaffHierarchyScore") val averageStaffHierarchyScore: Double = 0.0,
    @get:PropertyName("averageStaffMessageShare") @field:PropertyName("averageStaffMessageShare") val averageStaffMessageShare: Double = 0.0,
    @get:PropertyName("averageStaffResponseShare") @field:PropertyName("averageStaffResponseShare") val averageStaffResponseShare: Double = 0.0,

    @get:PropertyName("veryHighIssueCount") @field:PropertyName("veryHighIssueCount") val veryHighIssueCount: Long = 0L,
    @get:PropertyName("highIssueCount") @field:PropertyName("highIssueCount") val highIssueCount: Long = 0L,
    @get:PropertyName("mediumIssueCount") @field:PropertyName("mediumIssueCount") val mediumIssueCount: Long = 0L,
    @get:PropertyName("lowIssueCount") @field:PropertyName("lowIssueCount") val lowIssueCount: Long = 0L,

    @get:PropertyName("systemHierarchyLevel") @field:PropertyName("systemHierarchyLevel") val systemHierarchyLevel: String = "LOW",
    @get:PropertyName("updatedAt") @field:PropertyName("updatedAt") val updatedAt: Long = 0L
)
