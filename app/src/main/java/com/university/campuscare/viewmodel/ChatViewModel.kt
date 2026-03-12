package com.university.campuscare.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.data.model.Issue
import com.university.campuscare.data.model.IssueConversationInsights
import com.university.campuscare.data.model.IssueStatus
import com.university.campuscare.data.model.Message
import com.university.campuscare.data.model.Notification
import com.university.campuscare.data.model.NotificationType
import com.university.campuscare.data.repository.ChatRepository
import com.university.campuscare.data.repository.ChatRepositoryImpl
import com.university.campuscare.data.repository.IssueInsightsRepository
import com.university.campuscare.data.repository.NotificationRepository
import com.university.campuscare.data.repository.NotificationRepositoryImpl
import com.university.campuscare.data.repository.StaffInsightsRepository
import com.university.campuscare.domain.IssueInsightsCalculator
import com.university.campuscare.domain.StaffHierarchyCalculator
import com.university.campuscare.utils.DataResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatViewModel : ViewModel() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val chatRepository: ChatRepository = ChatRepositoryImpl(firestore)
    private val notificationRepository: NotificationRepository =
        NotificationRepositoryImpl(firestore)
    private val issueInsightsRepository = IssueInsightsRepository(firestore)
    private val staffInsightsRepository = StaffInsightsRepository(firestore)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _issueStatus = MutableStateFlow<IssueStatus?>(null)
    val issueStatus: StateFlow<IssueStatus?> = _issueStatus.asStateFlow()

    private var messagesJob: Job? = null
    private var insightsJob: Job? = null

    fun loadMessages(issueId: String) {
        refreshIssueStatus(issueId)

        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.getMessages(issueId).collect { result ->
                when (result) {
                    is DataResult.Loading -> {
                        _isLoading.value = true
                    }

                    is DataResult.Success -> {
                        _messages.value = result.data
                        _isLoading.value = false
                    }

                    is DataResult.Error -> {
                        Log.e(
                            "ChatViewModel",
                            "Error loading messages: ${result.error.peekContent()}"
                        )
                        _error.value = result.error.peekContent()
                        _isLoading.value = false
                    }

                    else -> Unit
                }
            }
        }
    }

    fun refreshIssueStatus(issueId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("reports").document(issueId).get().await()
                val issue = snapshot.toObject(Issue::class.java)
                _issueStatus.value = issue?.status
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to refresh issue status", e)
            }
        }
    }

    private fun createNotification(
        title: String,
        message: String,
        issueId: String,
        reportedBy: String
    ) {
        viewModelScope.launch {
            val newNotification = Notification(
                type = NotificationType.NEW_MESSAGE,
                title = title,
                message = message,
                issueId = issueId,
                timestamp = System.currentTimeMillis()
            )
            notificationRepository.createNotification(reportedBy, newNotification).collect { }
        }
    }

    fun startIssueInsights(issueId: String) {
        insightsJob?.cancel()
        insightsJob = viewModelScope.launch {
            messages
                .debounce(800)
                .collect { list ->
                    try {
                        val snapshot = firestore.collection("reports").document(issueId).get().await()
                        val issue = snapshot.toObject(Issue::class.java)

                        val assignedStaffId = issue?.assignedTo ?: ""
                        val assignedStaffName = issue?.assignedToName ?: ""

                        val computed = IssueInsightsCalculator.compute(
                            issueId = issueId,
                            messages = list,
                            assignedStaffId = assignedStaffId,
                            assignedStaffName = assignedStaffName
                        )

                        issueInsightsRepository.upsert(computed)
                        recomputeSystemStaffHierarchy()

                        Log.d(
                            "ISSUE_INSIGHTS",
                            "Upsert OK issueId=$issueId total=${computed.totalMessages} staff=${computed.assignedStaffId}"
                        )
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Failed to upsert issue insights", e)
                    }
                }
        }
    }

    private suspend fun recomputeSystemStaffHierarchy() {
        try {
            val snapshot = firestore.collection("issue_insights").get().await()

            val allInsights = snapshot.documents.mapNotNull { doc ->
                doc.toObject(IssueConversationInsights::class.java)
            }

            val grouped = allInsights
                .filter { it.assignedStaffId.isNotBlank() }
                .groupBy { it.assignedStaffId }

            for ((staffId, staffIssues) in grouped) {
                val staffName = staffIssues.firstOrNull()?.assignedStaffName ?: ""

                val computed = StaffHierarchyCalculator.compute(
                    staffId = staffId,
                    staffName = staffName,
                    issueInsights = staffIssues
                )

                staffInsightsRepository.upsert(computed)
            }

            Log.d(
                "STAFF_HIERARCHY",
                "System staff hierarchy recomputed for ${grouped.size} staff members"
            )
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Failed to recompute system staff hierarchy", e)
        }
    }

    fun sendMessage(
        issueId: String,
        senderId: String,
        senderName: String,
        text: String,
        isAdmin: Boolean
    ) {
        if (text.isBlank()) return

        val newMessage = Message(
            id = UUID.randomUUID().toString(),
            issueId = issueId,
            senderId = senderId,
            senderName = senderName,
            message = text,
            isFromAdmin = isAdmin,
            timestamp = System.currentTimeMillis()
        )

        val currentList = _messages.value.toMutableList()
        currentList.add(newMessage)
        _messages.value = currentList

        viewModelScope.launch {
            val result = chatRepository.sendMessage(newMessage)

            if (result is DataResult.Error) {
                _error.value = result.error.peekContent()
                Log.e(
                    "ChatViewModel",
                    "Failed to send message: ${result.error.peekContent()}"
                )
            } else {
                try {
                    val snapshot = firestore.collection("reports").document(issueId).get().await()
                    val issue = snapshot.toObject(Issue::class.java)

                    if (issue != null && !isAdmin) {
                        val notificationMessage =
                            "You have a new message in chat for the issue \"${issue.title}\"."
                        val notificationTitle = "New chat message"
                        createNotification(
                            notificationTitle,
                            notificationMessage,
                            issueId,
                            issue.reportedBy
                        )
                    }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Error sending notification: ${e.message}")
                }
            }
        }
    }
}