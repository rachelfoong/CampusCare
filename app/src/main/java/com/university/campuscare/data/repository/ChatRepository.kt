package com.university.campuscare.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.university.campuscare.data.model.Message
import com.university.campuscare.utils.DataResult
import com.university.campuscare.utils.Event
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface ChatRepository {
    fun getMessages(issueId: String): Flow<DataResult<List<Message>>>
    suspend fun sendMessage(message: Message): DataResult<Unit>
}

class ChatRepositoryImpl(
    private val firestore: FirebaseFirestore
) : ChatRepository {

    private val messagesCollection = firestore.collection("chats")

    override fun getMessages(issueId: String): Flow<DataResult<List<Message>>> = callbackFlow {
        trySend(DataResult.Loading)

        val subscription = messagesCollection
            .whereEqualTo("issueId", issueId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DataResult.Error(Event(error.message ?: "Failed to fetch messages")))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.toObjects(Message::class.java)
                    trySend(DataResult.Success(messages))
                }
            }

        awaitClose { subscription.remove() }
    }

    override suspend fun sendMessage(message: Message): DataResult<Unit> {
        return try {
            val docRef = messagesCollection.document()
            val messageWithId = message.copy(id = docRef.id, timestamp = System.currentTimeMillis())
            docRef.set(messageWithId).await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(Event(e.message ?: "Failed to send message"))
        }
    }
}
