package com.university.campuscare.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.data.model.Issue
import com.university.campuscare.data.model.IssueStatus
import com.university.campuscare.data.model.IssueCategory
import com.university.campuscare.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.university.campuscare.data.repository.IssuesRepositoryImpl
import com.university.campuscare.utils.DataResult
import com.university.campuscare.data.repository.NotificationRepositoryImpl
import com.university.campuscare.data.model.Notification
import com.university.campuscare.data.model.NotificationType
import kotlinx.coroutines.tasks.await

data class AdminStats(
    val total: Int = 0,
    val pending: Int = 0,
    val active: Int = 0,
    val resolved: Int = 0
)
// TODO FOR ADMIN FUNCTIONS:
// Tap on an issue to go to a detailed view screen
// Access the corresponding chat from the issue
// Admin dashboard analytics tab
class AdminViewModel : ViewModel() {

    private val _allIssues = MutableStateFlow<List<Issue>>(emptyList())
    val allIssues: StateFlow<List<Issue>> = _allIssues.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val issuesRepository = IssuesRepositoryImpl(firestore)
    private val notificationRepository = NotificationRepositoryImpl(firestore)

    private val _stats = MutableStateFlow(AdminStats())
    val stats: StateFlow<AdminStats> = _stats.asStateFlow()
    
    private val _selectedFilter = MutableStateFlow<IssueStatus?>(null)
    val selectedFilter: StateFlow<IssueStatus?> = _selectedFilter.asStateFlow()

    private val _selectedCategory = MutableStateFlow<IssueCategory?>(null)
    val selectedCategory: StateFlow<IssueCategory?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadAllIssues()
        loadAllUsers()
    }

    // Get all issues to display to the admin
    fun loadAllIssues() {
        viewModelScope.launch {
            issuesRepository.getAllIssues().collect { result ->
                when(result) {
                    is DataResult.Success -> {
                        _allIssues.value = result.data
                        updateStats()
                        _isLoading.value = false
                    }
                    is DataResult.Error -> {
                        _isLoading.value = false
                        Log.e("AdminViewModel", "Error loading issues: ${result.error.peekContent()}")
                    }
                    is DataResult.Loading -> {
                        _isLoading.value = true
                    }
                    else -> {}
                }
            }
        }
    }

    // Get all users for display to the admin
    fun loadAllUsers() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").get().await()
                val users = snapshot.toObjects(User::class.java)
                _allUsers.value = users
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error loading users: ${e.message}")
            }
        }
    }

    // Admin dashboard stats
    private fun updateStats() {
        val issues = _allIssues.value
        _stats.value = AdminStats(
            total = issues.size,
            pending = issues.count { it.status == IssueStatus.PENDING },
            active = issues.count { it.status == IssueStatus.IN_PROGRESS },
            resolved = issues.count { it.status == IssueStatus.RESOLVED }
        )
    }
    
    fun setFilter(status: IssueStatus?) {
        _selectedFilter.value = status
    }

    fun setCategoryFilter(category: IssueCategory?) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Create notification in firebase using notifications repo
    private fun createNotification(title: String, notificationType: NotificationType,message: String, issueId: String, reportedBy: String) {
        viewModelScope.launch {
            val newNotification = Notification(
                type = notificationType,
                title = title,
                message = message,
                issueId = issueId,
                timestamp = System.currentTimeMillis()
            )
            notificationRepository.createNotification(reportedBy, newNotification).collect { _ -> }
        }
    }

    // Update issue status in firebase and create notification
    private fun updateIssueStatus(issueId: String, notificationType: NotificationType, newStatus: IssueStatus, notificationTitle: String, notificationMessage: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val docRef = firestore.collection("reports").document(issueId)
                docRef.update(
                    mapOf(
                        "status" to newStatus.name,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()

                val snapshot = docRef.get().await()
                val issue = snapshot.toObject(Issue::class.java)

                if (issue != null) {
                    val finalMessage = notificationMessage.format(issue.title)
                    createNotification(notificationTitle, notificationType,finalMessage, issueId, issue.reportedBy)
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error updating issue status: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Mark issue as accepted from admin screen
    fun acceptIssue(issueId: String) {
        val acceptanceMessageTemplate = "Your issue \"%s\" has been reviewed and is now in progress."
        val acceptanceNotificationTitle = "Issue accepted for review"
        updateIssueStatus(issueId, NotificationType.STATUS_UPDATE,IssueStatus.IN_PROGRESS, acceptanceNotificationTitle, acceptanceMessageTemplate)
    }

    // Mark issue as resolved from admin screen
    fun resolveIssue(issueId: String) {
        val resolvedMessageTemplate = "Your issue \"%s\" has been resolved."
        val resolvedNotificationTitle = "Issue resolved"
        updateIssueStatus(issueId, NotificationType.ISSUE_RESOLVED, IssueStatus.RESOLVED, resolvedNotificationTitle, resolvedMessageTemplate)
    }

    fun getFilteredIssues(): List<Issue> {
        var filtered = _allIssues.value
        
        _selectedFilter.value?.let { status ->
            filtered = filtered.filter { it.status == status }
        }

        _selectedCategory.value?.let { category ->
            filtered = filtered.filter { it.category.equals(category.name, ignoreCase = true) }
        }

        if (_searchQuery.value.isNotBlank()) {
            filtered = filtered.filter { issue ->
                issue.title.contains(_searchQuery.value, ignoreCase = true) ||
                issue.description.contains(_searchQuery.value, ignoreCase = true) ||
                issue.reporterName.contains(_searchQuery.value, ignoreCase = true)
            }
        }
        
        return filtered
    }
}
