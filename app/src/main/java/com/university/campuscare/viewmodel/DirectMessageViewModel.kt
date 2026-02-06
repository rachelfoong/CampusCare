package com.university.campuscare.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.data.model.User
import com.university.campuscare.data.repository.DirectMessage
import com.university.campuscare.data.repository.DirectMessageRepository
import com.university.campuscare.data.repository.DirectMessageRepositoryImpl
import com.university.campuscare.utils.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DirectMessageViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: DirectMessageRepository = DirectMessageRepositoryImpl(
        FirebaseFirestore.getInstance()
    )

    // Admin users list
    private val _adminUsers = MutableStateFlow<DataResult<List<User>>>(DataResult.Loading)
    val adminUsers: StateFlow<DataResult<List<User>>> = _adminUsers

    // Messages for current conversation
    private val _messages = MutableStateFlow<List<DirectMessage>>(emptyList())
    val messages: StateFlow<List<DirectMessage>> = _messages

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadAdminUsers() {
        viewModelScope.launch {
            _adminUsers.value = DataResult.Loading
            _adminUsers.value = repository.getAdminUsers()
        }
    }

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            repository.getMessages(conversationId).collect { result ->
                when (result) {
                    is DataResult.Loading -> {
                        _isLoading.value = true
                    }
                    is DataResult.Success -> {
                        _messages.value = result.data
                        _isLoading.value = false
                        _error.value = null
                    }
                    is DataResult.Error -> {
                        _error.value = result.error.peekContent()
                        _isLoading.value = false
                    }
                    else -> {}
                }
            }
        }
    }

    fun sendMessage(conversationId: String, message: DirectMessage) {
        viewModelScope.launch {
            val result = repository.sendMessage(conversationId, message)
            if (result is DataResult.Error) {
                _error.value = result.error.peekContent()
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
