package com.university.campuscare.data.model

data class StaffHierarchyInsight(
    val staffId: String = "",
    val staffName: String = "",

    val issuesHandled: Long = 0L,

    val averageStaffHierarchyScore: Double = 0.0,
    val averageStaffMessageShare: Double = 0.0,
    val averageStaffResponseShare: Double = 0.0,

    val veryHighIssueCount: Long = 0L,
    val highIssueCount: Long = 0L,
    val mediumIssueCount: Long = 0L,
    val lowIssueCount: Long = 0L,

    val systemHierarchyLevel: String = "LOW",
    val updatedAt: Long = 0L
)