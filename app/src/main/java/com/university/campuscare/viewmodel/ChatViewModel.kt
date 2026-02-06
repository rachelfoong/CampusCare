package com.university.campuscare.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.university.campuscare.data.model.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun loadMessages(issueId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                firestore.collection("issues")
                    .document(issueId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            _error.value = "Failed to load messages: ${e.message}"
                            _isLoading.value = false
                            return@addSnapshotListener
                        }
                        
                        if (snapshot != null) {
                            _messages.value = snapshot.documents.mapNotNull { doc ->
                                try {
                                    Message(
                                        id = doc.id,
                                        senderId = doc.getString("senderId") ?: "",
                                        senderName = doc.getString("senderName") ?: "",
                                        message = doc.getString("message") ?: "",
                                        timestamp = doc.getLong("timestamp") ?: 0L,
                                        isFromAdmin = doc.getBoolean("isFromAdmin") ?: false
                                    )
                                } catch (ex: Exception) {
                                    Log.e("ChatViewModel", "Error parsing message: ${ex.message}")
                                    null
                                }
                            }
                            _isLoading.value = false
                        }
                    }
            } catch (e: Exception) {
                _error.value = "Failed to load messages: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun sendMessage(
        issueId: String,
        senderId: String,
        senderName: String,
        text: String,
        isAdmin: Boolean
    ) {
        viewModelScope.launch {
            _error.value = null
            
            try {
                val messageData = hashMapOf(
                    "senderId" to senderId,
                    "senderName" to senderName,
                    "message" to text,
                    "timestamp" to System.currentTimeMillis(),
                    "isFromAdmin" to isAdmin
                )
                
                firestore.collection("issues")
                    .document(issueId)
                    .collection("messages")
                    .add(messageData)
                    .await()
                    
            } catch (e: Exception) {
                _error.value = "Failed to send message: ${e.message}"
                Log.e("ChatViewModel", "Error sending message: ${e.message}")
            }
        }
    }
}
