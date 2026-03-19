package com.university.campuscare.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.university.campuscare.data.model.IssueConversationInsights
import kotlinx.coroutines.tasks.await

class IssueInsightsRepository(
    private val firestore: FirebaseFirestore
) {
    private fun doc(issueId: String) =
        firestore.collection("issue_insights").document(issueId)

    suspend fun upsert(insights: IssueConversationInsights) {
        doc(insights.issueId)
            .set(insights)
            .await()
    }
}