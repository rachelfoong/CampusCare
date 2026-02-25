package com.university.campuscare.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.university.campuscare.data.model.ConversationInsights
import kotlinx.coroutines.tasks.await

class InsightsRepository(
    private val firestore: FirebaseFirestore
) {
    private fun doc(conversationId: String) =
        firestore.collection("conversation_insights").document(conversationId)

    suspend fun upsertInsights(insights: ConversationInsights) {
        doc(insights.conversationId)
            .set(insights, SetOptions.merge())
            .await()
    }
}