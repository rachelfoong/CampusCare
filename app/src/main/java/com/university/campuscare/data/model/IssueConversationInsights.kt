package com.university.campuscare.data.model

import com.google.firebase.firestore.PropertyName

data class IssueConversationInsights(
    @get:PropertyName("issueId") @field:PropertyName("issueId") val issueId: String = "",

    @get:PropertyName("totalMessages") @field:PropertyName("totalMessages") val totalMessages: Long = 0L,
    @get:PropertyName("speakerCount") @field:PropertyName("speakerCount") val speakerCount: Long = 0L,

    @get:PropertyName("dominantSenderId") @field:PropertyName("dominantSenderId") val dominantSenderId: String = "",
    @get:PropertyName("dominantSenderName") @field:PropertyName("dominantSenderName") val dominantSenderName: String = "",
    @get:PropertyName("dominantSenderMessageShare") @field:PropertyName("dominantSenderMessageShare") val dominantSenderMessageShare: Double = 0.0,

    @get:PropertyName("conversationConcentrationScore") @field:PropertyName("conversationConcentrationScore") val conversationConcentrationScore: Double = 0.0,
    @get:PropertyName("interactionHierarchyScore") @field:PropertyName("interactionHierarchyScore") val interactionHierarchyScore: Double = 0.0,

    @get:PropertyName("dominantResponderId") @field:PropertyName("dominantResponderId") val dominantResponderId: String = "",
    @get:PropertyName("dominantResponderName") @field:PropertyName("dominantResponderName") val dominantResponderName: String = "",
    @get:PropertyName("responsePowerScore") @field:PropertyName("responsePowerScore") val responsePowerScore: Double = 0.0,

    @get:PropertyName("activityScore") @field:PropertyName("activityScore") val activityScore: Double = 0.0,
    @get:PropertyName("burstsTotal") @field:PropertyName("burstsTotal") val burstsTotal: Long = 0L,

    @get:PropertyName("hierarchyCompositeScore") @field:PropertyName("hierarchyCompositeScore") val hierarchyCompositeScore: Double = 0.0,
    @get:PropertyName("hierarchyLevel") @field:PropertyName("hierarchyLevel") val hierarchyLevel: String = "FLAT",

    @get:PropertyName("assignedStaffId") @field:PropertyName("assignedStaffId") val assignedStaffId: String = "",
    @get:PropertyName("assignedStaffName") @field:PropertyName("assignedStaffName") val assignedStaffName: String = "",
    @get:PropertyName("assignedStaffMessageShare") @field:PropertyName("assignedStaffMessageShare") val assignedStaffMessageShare: Double = 0.0,
    @get:PropertyName("assignedStaffResponseShare") @field:PropertyName("assignedStaffResponseShare") val assignedStaffResponseShare: Double = 0.0,
    @get:PropertyName("assignedStaffHierarchyScore") @field:PropertyName("assignedStaffHierarchyScore") val assignedStaffHierarchyScore: Double = 0.0,
    @get:PropertyName("assignedStaffHierarchyLevel") @field:PropertyName("assignedStaffHierarchyLevel") val assignedStaffHierarchyLevel: String = "LOW",

    @get:PropertyName("updatedAt") @field:PropertyName("updatedAt") val updatedAt: Long = 0L
)
