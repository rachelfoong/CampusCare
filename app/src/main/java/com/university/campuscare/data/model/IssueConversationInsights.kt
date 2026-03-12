package com.university.campuscare.data.model

data class IssueConversationInsights(
    val issueId: String = "",

    val totalMessages: Long = 0L,
    val speakerCount: Long = 0L,

    val dominantSenderId: String = "",
    val dominantSenderName: String = "",
    val dominantSenderMessageShare: Double = 0.0,

    val conversationConcentrationScore: Double = 0.0,
    val interactionHierarchyScore: Double = 0.0,

    val dominantResponderId: String = "",
    val dominantResponderName: String = "",
    val responsePowerScore: Double = 0.0,

    val activityScore: Double = 0.0,
    val burstsTotal: Long = 0L,

    val hierarchyCompositeScore: Double = 0.0,
    val hierarchyLevel: String = "FLAT",

    val assignedStaffId: String = "",
    val assignedStaffName: String = "",
    val assignedStaffMessageShare: Double = 0.0,
    val assignedStaffResponseShare: Double = 0.0,
    val assignedStaffHierarchyScore: Double = 0.0,
    val assignedStaffHierarchyLevel: String = "LOW",

    val updatedAt: Long = 0L
)