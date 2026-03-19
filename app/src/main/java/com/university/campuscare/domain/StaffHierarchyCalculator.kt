package com.university.campuscare.domain

import com.university.campuscare.data.model.IssueConversationInsights
import com.university.campuscare.data.model.StaffHierarchyInsight

object StaffHierarchyCalculator {

    fun compute(
        staffId: String,
        staffName: String,
        issueInsights: List<IssueConversationInsights>
    ): StaffHierarchyInsight {

        if (staffId.isBlank() || issueInsights.isEmpty()) {
            return StaffHierarchyInsight(
                staffId = staffId,
                staffName = staffName,
                updatedAt = System.currentTimeMillis()
            )
        }

        val issuesHandled = issueInsights.size.toLong()

        val averageStaffHierarchyScore =
            issueInsights.map { it.assignedStaffHierarchyScore }.average()

        val averageStaffMessageShare =
            issueInsights.map { it.assignedStaffMessageShare }.average()

        val averageStaffResponseShare =
            issueInsights.map { it.assignedStaffResponseShare }.average()

        val veryHighIssueCount =
            issueInsights.count { it.assignedStaffHierarchyLevel == "VERY_HIGH" }.toLong()

        val highIssueCount =
            issueInsights.count { it.assignedStaffHierarchyLevel == "HIGH" }.toLong()

        val mediumIssueCount =
            issueInsights.count { it.assignedStaffHierarchyLevel == "MEDIUM" }.toLong()

        val lowIssueCount =
            issueInsights.count { it.assignedStaffHierarchyLevel == "LOW" }.toLong()

        val systemHierarchyLevel = when {
            issuesHandled < 2 -> "LOW"
            averageStaffHierarchyScore < 0.25 -> "LOW"
            averageStaffHierarchyScore < 0.50 -> "MEDIUM"
            averageStaffHierarchyScore < 0.75 -> "HIGH"
            else -> "VERY_HIGH"
        }

        return StaffHierarchyInsight(
            staffId = staffId,
            staffName = staffName,
            issuesHandled = issuesHandled,

            averageStaffHierarchyScore = averageStaffHierarchyScore,
            averageStaffMessageShare = averageStaffMessageShare,
            averageStaffResponseShare = averageStaffResponseShare,

            veryHighIssueCount = veryHighIssueCount,
            highIssueCount = highIssueCount,
            mediumIssueCount = mediumIssueCount,
            lowIssueCount = lowIssueCount,

            systemHierarchyLevel = systemHierarchyLevel,
            updatedAt = System.currentTimeMillis()
        )
    }
}