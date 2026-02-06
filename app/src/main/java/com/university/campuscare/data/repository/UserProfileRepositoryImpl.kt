package com.university.campuscare.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.data.model.User
import com.university.campuscare.utils.DataResult
import com.university.campuscare.utils.Event
import kotlinx.coroutines.tasks.await

class UserProfileRepositoryImpl(
    private val firestore: FirebaseFirestore
) : UserProfileRepository {

    override suspend fun getUserProfile(userId: String): DataResult<User> {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            val user = document.toObject(User::class.java)
            if (user != null) {
                DataResult.Success(user)
            } else {
                DataResult.Error(Event("User not found"))
            }
        } catch (e: Exception) {
            DataResult.Error(Event(e.message ?: "An unknown error occurred"))
        }
    }

    override suspend fun updateUserProfile(user: User): DataResult<Unit> {
        return try {
            firestore.collection("users").document(user.userId).set(user).await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(Event(e.message ?: "An unknown error occurred"))
        }
    }
}
