package com.university.campuscare.domain
import com.university.campuscare.data.model.ConversationInsights
import com.university.campuscare.data.repository.DirectMessage
import kotlin.math.ln

object ConversationInsightsCalculator {

    private const val BURST_GAP_MS = 20 * 60 * 1000L // 20 minutes

    fun compute(
        conversationId: String,
        messages: List<DirectMessage>,
        uidA: String,
        uidB: String
    ): ConversationInsights {
        val sorted = messages.sortedBy { it.timestamp }

        val aSent = sorted.count { it.senderId == uidA }.toLong()
        val bSent = sorted.count { it.senderId == uidB }.toLong()
        val total = sorted.size.toLong()

        val reciprocity = reciprocity(aSent, bSent)
        val medianReplyOverall = medianReplyTimeOverall(sorted)
        val burst = burstStats(sorted, uidA, uidB)

        val activityScore = activityScore(
            totalMessages = total,
            reciprocity = reciprocity,
            medianReplyTimeMs = medianReplyOverall
        )

        return ConversationInsights(
            conversationId = conversationId,
            userA = uidA,
            userB = uidB,
            totalMessages = total,
            aSent = aSent,
            bSent = bSent,
            reciprocity = reciprocity,
            medianReplyTimeMs = medianReplyOverall,
            activityScore = activityScore,
            burstsTotal = burst.total,
            aBurstStarts = burst.aStarts,
            bBurstStarts = burst.bStarts,
            aLastWord = burst.aLast,
            bLastWord = burst.bLast,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun reciprocity(aSent: Long, bSent: Long): Double {
        val max = maxOf(aSent, bSent)
        val min = minOf(aSent, bSent)
        return if (max == 0L) 0.0 else min.toDouble() / max.toDouble()
    }

    private fun medianReplyTimeOverall(sorted: List<DirectMessage>): Long? {
        if (sorted.size < 2) return null
        val replyTimes = mutableListOf<Long>()
        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val curr = sorted[i]
            if (prev.senderId.isBlank() || curr.senderId.isBlank()) continue
            if (prev.senderId == curr.senderId) continue
            val dt = curr.timestamp - prev.timestamp
            if (dt > 0) replyTimes.add(dt)
        }
        if (replyTimes.isEmpty()) return null
        replyTimes.sort()
        val mid = replyTimes.size / 2
        return if (replyTimes.size % 2 == 1) replyTimes[mid] else (replyTimes[mid - 1] + replyTimes[mid]) / 2
    }

    private data class BurstStats(
        val total: Long,
        val aStarts: Long,
        val bStarts: Long,
        val aLast: Long,
        val bLast: Long
    )

    private fun burstStats(sorted: List<DirectMessage>, uidA: String, uidB: String): BurstStats {
        if (sorted.isEmpty()) return BurstStats(0, 0, 0, 0, 0)

        var total = 0L
        var aStarts = 0L
        var bStarts = 0L
        var aLast = 0L
        var bLast = 0L

        var prevTs: Long? = null
        var burstStartSender: String? = null
        var lastSender: String? = null

        fun closeBurst() {
            val start = burstStartSender ?: return
            val last = lastSender ?: return
            total += 1
            if (start == uidA) aStarts += 1 else if (start == uidB) bStarts += 1
            if (last == uidA) aLast += 1 else if (last == uidB) bLast += 1
        }

        for (m in sorted) {
            val isNewBurst = prevTs == null || (m.timestamp - prevTs!!) > BURST_GAP_MS
            if (isNewBurst) {
                if (prevTs != null) closeBurst()
                burstStartSender = m.senderId
            }
            lastSender = m.senderId
            prevTs = m.timestamp
        }
        closeBurst()

        return BurstStats(total, aStarts, bStarts, aLast, bLast)
    }

    private fun activityScore(
        totalMessages: Long,
        reciprocity: Double,
        medianReplyTimeMs: Long?
    ): Double {
        val volume = (ln((totalMessages + 1).toDouble()) / ln(50.0)).coerceIn(0.0, 1.0)

        val responsiveness = when (medianReplyTimeMs) {
            null -> 0.0
            else -> {
                val minutes = medianReplyTimeMs / 60000.0
                ((60.0 - minutes).coerceIn(0.0, 60.0) / 60.0)
            }
        }

        return (0.45 * volume) + (0.35 * reciprocity) + (0.20 * responsiveness)
    }
}