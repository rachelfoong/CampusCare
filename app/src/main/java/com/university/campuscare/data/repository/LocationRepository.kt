package com.university.campuscare.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.data.model.Location
import kotlinx.coroutines.tasks.await

class LocationRepository(private val firestore: FirebaseFirestore) {

    suspend fun saveLocationLog(location: Location) {
        val userId = location.userId

        val logRef = firestore
            .collection("location_logs")
            .document(userId)
            .collection("logs")
            .document()

        val logWithId = location.copy(id = logRef.id)

        logRef.set(logWithId).await()
    }
}