package com.university.campuscare.data.repository

import com.university.campuscare.data.model.User
import com.university.campuscare.utils.DataResult
import kotlinx.coroutines.flow.Flow

data class DirectMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)

interface DirectMessageRepository {
    suspend fun getAdminUsers(): DataResult<List<User>>
    fun getMessages(conversationId: String): Flow<DataResult<List<DirectMessage>>>
    suspend fun sendMessage(conversationId: String, message: DirectMessage): DataResult<Unit>
}
