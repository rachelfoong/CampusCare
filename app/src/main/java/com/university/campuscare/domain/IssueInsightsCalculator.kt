package com.university.campuscare.domain

import com.university.campuscare.data.model.IssueConversationInsights
import com.university.campuscare.data.model.Message
import kotlin.math.ln

object IssueInsightsCalculator {

    private const val BURST_GAP_MS = 20 * 60 * 1000L // 20 minutes

    fun compute(
        issueId: String,
        messages: List<Message>,
        assignedStaffId: String,
        assignedStaffName: String
    ): IssueConversationInsights {
        val sorted = messages.sortedBy { it.timestamp }
        val total = sorted.size.toLong()

        val burst = burstStats(sorted)
        val activityScore = activityScore(totalMessages = total)

        val senderGroups = sorted.groupBy { it.senderId }
        val speakerCount = senderGroups.size.toLong()

        val dominantEntry = senderGroups.maxByOrNull { it.value.size }
        val dominantSenderId = dominantEntry?.key ?: ""
        val dominantSenderName = dominantEntry?.value?.firstOrNull()?.senderName ?: ""
        val dominantSenderCount = dominantEntry?.value?.size?.toLong() ?: 0L

        val dominantSenderMessageShare =
            if (total == 0L) 0.0 else dominantSenderCount.toDouble() / total.toDouble()

        val conversationConcentrationScore = if (total == 0L || speakerCount <= 1L) {
            0.0
        } else {
            senderGroups.values.sumOf { msgs ->
                val share = msgs.size.toDouble() / total.toDouble()
                share * share
            }
        }

        val interactionHierarchyScore =
            when {
                total <= 1L || speakerCount <= 1L -> 0.0
                else -> (
                        (dominantSenderMessageShare - (1.0 / speakerCount.toDouble()))
                            .coerceAtLeast(0.0) /
                                (1.0 - (1.0 / speakerCount.toDouble()))
                        ).coerceIn(0.0, 1.0)
            }

        val responseCounts = mutableMapOf<String, Int>()

        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val curr = sorted[i]

            if (prev.senderId != curr.senderId) {
                responseCounts[prev.senderId] = (responseCounts[prev.senderId] ?: 0) + 1
            }
        }

        val dominantResponderEntry = responseCounts.maxByOrNull { it.value }
        val dominantResponderId = dominantResponderEntry?.key ?: ""
        val dominantResponderName =
            sorted.firstOrNull { it.senderId == dominantResponderId }?.senderName ?: ""

        val totalResponses = responseCounts.values.sum()

        val responsePowerScore =
            if (totalResponses == 0) 0.0
            else (dominantResponderEntry?.value?.toDouble() ?: 0.0) / totalResponses.toDouble()

        val hierarchyCompositeScore =
            (
                    interactionHierarchyScore * 0.6 +
                            responsePowerScore * 0.4
                    ).coerceIn(0.0, 1.0)

        val hierarchyLevel = when {
            total <= 1L || speakerCount <= 1L -> "FLAT"
            hierarchyCompositeScore < 0.25 -> "FLAT"
            hierarchyCompositeScore < 0.50 -> "LOW"
            hierarchyCompositeScore < 0.75 -> "MODERATE"
            else -> "HIGH"
        }

        val staffMessageCount = senderGroups[assignedStaffId]?.size?.toLong() ?: 0L
        val assignedStaffMessageShare =
            if (total == 0L || assignedStaffId.isBlank()) 0.0
            else staffMessageCount.toDouble() / total.toDouble()

        val staffTriggeredResponses = responseCounts[assignedStaffId] ?: 0
        val assignedStaffResponseShare =
            if (totalResponses == 0 || assignedStaffId.isBlank()) 0.0
            else staffTriggeredResponses.toDouble() / totalResponses.toDouble()

        val assignedStaffHierarchyScore =
            (
                    assignedStaffMessageShare * 0.5 +
                            assignedStaffResponseShare * 0.5
                    ).coerceIn(0.0, 1.0)

        val assignedStaffHierarchyLevel = when {
            assignedStaffId.isBlank() -> "LOW"
            assignedStaffHierarchyScore < 0.25 -> "LOW"
            assignedStaffHierarchyScore < 0.50 -> "MEDIUM"
            assignedStaffHierarchyScore < 0.75 -> "HIGH"
            else -> "VERY_HIGH"
        }

        return IssueConversationInsights(
            issueId = issueId,
            totalMessages = total,
            speakerCount = speakerCount,

            dominantSenderId = dominantSenderId,
            dominantSenderName = dominantSenderName,
            dominantSenderMessageShare = dominantSenderMessageShare,

            conversationConcentrationScore = conversationConcentrationScore,
            interactionHierarchyScore = interactionHierarchyScore,

            dominantResponderId = dominantResponderId,
            dominantResponderName = dominantResponderName,
            responsePowerScore = responsePowerScore,

            activityScore = activityScore,
            burstsTotal = burst.total,

            hierarchyCompositeScore = hierarchyCompositeScore,
            hierarchyLevel = hierarchyLevel,

            assignedStaffId = assignedStaffId,
            assignedStaffName = assignedStaffName,
            assignedStaffMessageShare = assignedStaffMessageShare,
            assignedStaffResponseShare = assignedStaffResponseShare,
            assignedStaffHierarchyScore = assignedStaffHierarchyScore,
            assignedStaffHierarchyLevel = assignedStaffHierarchyLevel,

            updatedAt = System.currentTimeMillis()
        )
    }

    private data class BurstStats(
        val total: Long
    )

    private fun burstStats(sorted: List<Message>): BurstStats {
        if (sorted.isEmpty()) return BurstStats(0)

        var total = 0L
        var prevTs: Long? = null

        for (m in sorted) {
            val isNewBurst = prevTs == null || (m.timestamp - prevTs) > BURST_GAP_MS
            if (isNewBurst) total += 1
            prevTs = m.timestamp
        }

        return BurstStats(total)
    }

    private fun activityScore(totalMessages: Long): Double {
        return (ln((totalMessages + 1).toDouble()) / ln(50.0)).coerceIn(0.0, 1.0)
    }
}