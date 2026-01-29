package com.university.campuscare.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.data.model.User
import com.university.campuscare.utils.DataResult
import com.university.campuscare.utils.Event
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface StaffRepository {
    fun getAllStaff(): Flow<DataResult<List<User>>>
    fun getStaffByDepartment(department: String): Flow<DataResult<List<User>>>
}

class StaffRepositoryImpl(
    private val firestore: FirebaseFirestore
) : StaffRepository {

    private val usersCollection = firestore.collection("users")

    override fun getAllStaff(): Flow<DataResult<List<User>>> = callbackFlow {
        trySend(DataResult.Loading)

        val subscription = usersCollection
            .whereIn("role", listOf("STAFF", "ADMIN"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DataResult.Error(Event(error.message ?: "Failed to fetch staff directory")))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val staff = snapshot.toObjects(User::class.java)
                    trySend(DataResult.Success(staff))
                }
            }

        awaitClose { subscription.remove() }
    }

    override fun getStaffByDepartment(department: String): Flow<DataResult<List<User>>> = callbackFlow {
        trySend(DataResult.Loading)

        val subscription = usersCollection
            .whereIn("role", listOf("STAFF", "ADMIN"))
            .whereEqualTo("department", department)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DataResult.Error(Event(error.message ?: "Failed to fetch staff for $department")))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val staff = snapshot.toObjects(User::class.java)
                    trySend(DataResult.Success(staff))
                }
            }

        awaitClose { subscription.remove() }
    }
}
