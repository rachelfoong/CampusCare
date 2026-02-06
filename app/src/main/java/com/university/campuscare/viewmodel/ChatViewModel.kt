package com.university.campuscare.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.university.campuscare.data.model.Issue
import com.university.campuscare.data.model.Message
import com.university.campuscare.data.model.Notification
import com.university.campuscare.data.model.NotificationType
import com.university.campuscare.data.repository.ChatRepository
import com.university.campuscare.data.repository.ChatRepositoryImpl
import com.university.campuscare.data.repository.NotificationRepository
import com.university.campuscare.data.repository.NotificationRepositoryImpl
import com.university.campuscare.utils.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val chatRepository: ChatRepository = ChatRepositoryImpl(firestore)
    private val notificationRepository: NotificationRepository = NotificationRepositoryImpl(firestore)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // load messages by issueid
    fun loadMessages(issueId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(issueId).collect { result ->
                when (result) {
                    is DataResult.Loading -> _isLoading.value = true
                    is DataResult.Success -> {
                        _messages.value = result.data
                        _isLoading.value = false
                    }
                    is DataResult.Error -> {
                        Log.e("ChatViewModel", "Error loading messages: ${result.error.peekContent()}")
                        _error.value = result.error.peekContent()
                        _isLoading.value = false
                    }
                    else -> {}
                }
            }
        }
    }

    private fun createNotification(title: String, message: String, issueId: String, reportedBy: String) {
        viewModelScope.launch {
            val newNotification = Notification(
                type = NotificationType.NEW_MESSAGE,
                title = title,
                message = message,
                issueId = issueId,
                timestamp = System.currentTimeMillis()
            )
            notificationRepository.createNotification(reportedBy, newNotification).collect { _ -> }
        }
    }

    // send message in issue chat
    fun sendMessage(
        issueId: String, senderId: String, senderName: String, text: String, isAdmin: Boolean
    ) {
        if (text.isBlank()) return

        val newMessage = Message(
            id = UUID.randomUUID().toString(), // Temporary ID for UI
            issueId = issueId,
            senderId = senderId,
            senderName = senderName,
            message = text,
            isFromAdmin = isAdmin,
            timestamp = System.currentTimeMillis()
        )

        // OPTIMISTIC UPDATE: Add to list immediately
        val currentList = _messages.value.toMutableList()
        currentList.add(newMessage)
        _messages.value = currentList

        viewModelScope.launch {
            val result = chatRepository.sendMessage(newMessage)
            if (result is DataResult.Error) {
                _error.value = result.error.peekContent()
                Log.e("ChatViewModel", "Failed to send message: ${result.error.peekContent()}")

                // Optional: Remove message from list if failed
                // _messages.value = _messages.value.filter { it.id != newMessage.id }
            } else {
                try {
                    // Get the issue that the chat is associated with
                    val docRef = firestore.collection("reports").document(issueId)
                    val snapshot = docRef.get().await()
                    val issue = snapshot.toObject(Issue::class.java)

                    // Create the user notification
                    if (issue != null && !isAdmin) {
                        val notificationMessage = "You have a new message in chat for the issue \"${issue.title}\"."
                        val notificationTitle = "New chat message"
                        createNotification(notificationTitle, notificationMessage, issueId, issue.reportedBy)
                    }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Error sending notification: ${e.message}")
                }

            }
        }
    }
}
