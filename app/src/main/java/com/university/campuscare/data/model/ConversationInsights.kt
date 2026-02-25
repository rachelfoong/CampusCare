package com.university.campuscare.data.model

data class ConversationInsights(
    val conversationId: String = "",
    val userA: String = "",
    val userB: String = "",

    val totalMessages: Long = 0L,
    val aSent: Long = 0L,
    val bSent: Long = 0L,

    val reciprocity: Double = 0.0,
    val medianReplyTimeMs: Long? = null,
    val activityScore: Double = 0.0,

    // Optional but useful for "hierarchy later"
    val burstsTotal: Long = 0L,
    val aBurstStarts: Long = 0L,
    val bBurstStarts: Long = 0L,
    val aLastWord: Long = 0L,
    val bLastWord: Long = 0L,

    val updatedAt: Long = 0L
)