package com.university.campuscare.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.data.model.ClientProfile
import kotlinx.coroutines.tasks.await

// Internal collection — stores per-user session snapshots
// Firestore path: _sys/{userId}/sessions/{sessionId}
//
// --- SYUKRI: extend from here for Part 2 profiling history ---
// 1. Add getSessions(userId): Flow<List<ClientProfile>>
//    → query _sys/{userId}/sessions ordered by timestamp desc
// 2. Build an internal viewer screen (not in main nav) to display
//    the full history per user for demo purposes
// 3. Optionally add deleteSession() / clearHistory() for cleanup
// -------------------------------------------------------------

object ClientProfileRepository {

    private const val ROOT = "_sys"
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun pushSnapshot(profile: ClientProfile) {
        try {
            firestore
                .collection(ROOT)
                .document(profile.userId)
                .collection("sessions")
                .document(profile.sessionId)
                .set(profile)
                .await()
        } catch (_: Exception) {
            // Fail silently — must not affect normal app flow
        }
    }
}
