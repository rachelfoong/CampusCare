package com.university.campuscare.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.university.campuscare.data.model.User
import com.university.campuscare.utils.DataResult
import com.university.campuscare.utils.Event
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DirectMessageRepositoryImpl(
    private val firestore: FirebaseFirestore
) : DirectMessageRepository {

    override suspend fun getAdminUsers(): DataResult<List<User>> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("role", "ADMIN")
                .get()
                .await()
            
            val adminUsers = snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)
            }
            DataResult.Success(adminUsers)
        } catch (e: Exception) {
            DataResult.Error(Event(e.message ?: "Failed to load admins"))
        }
    }

    override fun getMessages(conversationId: String): Flow<DataResult<List<DirectMessage>>> = callbackFlow {
        val listener = firestore.collection("direct_messages")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(DataResult.Error(Event(e.message ?: "Failed to load messages")))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        DirectMessage(
                            id = doc.id,
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            receiverId = doc.getString("receiverId") ?: "",
                            message = doc.getString("message") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    }
                    trySend(DataResult.Success(messages))
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun sendMessage(conversationId: String, message: DirectMessage): DataResult<Unit> {
        return try {
            val messageData = hashMapOf(
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "receiverId" to message.receiverId,
                "message" to message.message,
                "timestamp" to message.timestamp
            )

            firestore.collection("direct_messages")
                .document(conversationId)
                .collection("messages")
                .add(messageData)
                .await()

            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(Event(e.message ?: "Failed to send message"))
        }
    }
}


