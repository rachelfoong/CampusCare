package com.university.campuscare.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.data.model.User
import com.university.campuscare.data.repository.DirectMessage
import com.university.campuscare.data.repository.DirectMessageRepository
import com.university.campuscare.data.repository.DirectMessageRepositoryImpl
import com.university.campuscare.data.repository.InsightsRepository
import com.university.campuscare.domain.ConversationInsightsCalculator
import com.university.campuscare.utils.DataResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class DirectMessageViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()

    private val repository: DirectMessageRepository = DirectMessageRepositoryImpl(firestore)
    private val insightsRepository = InsightsRepository(firestore)

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

    private var messagesJob: Job? = null
    private var insightsJob: Job? = null

    fun loadAdminUsers() {
        viewModelScope.launch {
            _adminUsers.value = DataResult.Loading
            _adminUsers.value = repository.getAdminUsers()
        }
    }

    fun loadMessages(conversationId: String) {
        // Prevent duplicate collectors if screen re-enters
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
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

    /**
     * Background analytics only:
     * - Computes conversation insights from current messages state
     * - Writes to: conversation_insights/{conversationId}
     * - Debounced to reduce Firestore writes
     */
    fun startConversationInsights(
        conversationId: String,
        currentUserId: String,
        otherUserId: String
    ) {
        val (uidA, uidB) = listOf(currentUserId, otherUserId).sorted()

        insightsJob?.cancel()
        insightsJob = viewModelScope.launch {
            messages
                .debounce(1200)
                .collect { list ->
                    val computed = ConversationInsightsCalculator.compute(
                        conversationId = conversationId,
                        messages = list,
                        uidA = uidA,
                        uidB = uidB
                    )
                    try {
                        insightsRepository.upsertInsights(computed)
                    } catch (_: Exception) {
                        // Do not break chat UX if analytics fails
                    }
                }
        }
    }

    fun sendMessage(conversationId: String, message: DirectMessage) {
        viewModelScope.launch {
            val result = repository.sendMessage(conversationId, message)
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            android.util.Log.d("AUTH_CHECK", "FirebaseAuth uid = $uid")
            if (result is DataResult.Error) {
                _error.value = result.error.peekContent()
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        messagesJob?.cancel()
        insightsJob?.cancel()
        super.onCleared()
    }
}