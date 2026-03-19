package com.university.campuscare.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.data.model.StaffHierarchyInsight
import kotlinx.coroutines.tasks.await

class StaffInsightsRepository(
    private val firestore: FirebaseFirestore
) {
    private fun doc(staffId: String) =
        firestore.collection("staff_insights").document(staffId)

    suspend fun upsert(insight: StaffHierarchyInsight) {
        doc(insight.staffId)
            .set(insight)
            .await()
    }
}