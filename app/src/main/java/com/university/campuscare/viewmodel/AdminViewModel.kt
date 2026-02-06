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
import java.util.Calendar

data class AdminStats(
    val total: Int = 0,
    val pending: Int = 0,
    val active: Int = 0,
    val resolved: Int = 0
)

data class AnalyticsData(
    val totalReportsThisMonth: Int = 0,
    val averageResolutionTime: String = "0 days",
    val mostReportedIssue: String = "None",
    val categoryBreakdown: Map<String, Int> = emptyMap()
)

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

    private val _analyticsData = MutableStateFlow(AnalyticsData())
    val analyticsData: StateFlow<AnalyticsData> = _analyticsData.asStateFlow()
    private val _selectedFilter = MutableStateFlow<IssueStatus?>(null)
    val selectedFilter: StateFlow<IssueStatus?> = _selectedFilter.asStateFlow()

    private val _selectedCategory = MutableStateFlow<IssueCategory?>(null)
    val selectedCategory: StateFlow<IssueCategory?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Staff list for assignment
    private val _staffList = MutableStateFlow<List<User>>(emptyList())
    val staffList: StateFlow<List<User>> = _staffList.asStateFlow()

    // Loading state for staff
    private val _isLoadingStaff = MutableStateFlow(false)
    val isLoadingStaff: StateFlow<Boolean> = _isLoadingStaff.asStateFlow()
    
    init {
        loadAllIssues()
        loadAllUsers()
        loadStaffMembers()
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

    // Load staff members for assignment
    fun loadStaffMembers() {
        viewModelScope.launch {
            try {
                _isLoadingStaff.value = true
                val snapshot = firestore.collection("users")
                    .whereEqualTo("role", "STAFF")
                    .get()
                    .await()

                val staff = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(userId = doc.id)
                }

                _staffList.value = staff
                Log.d("AdminViewModel", "Loaded ${staff.size} staff members")
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error loading staff: ${e.message}")
                _staffList.value = emptyList()
            } finally {
                _isLoadingStaff.value = false
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

        // Calculate analytics whenever stats are updated
        calculateAnalytics()  // ← ADD THIS LINE
    }

    fun calculateAnalytics() {
        viewModelScope.launch {
            try {
                val issues = _allIssues.value

                // 1. Total Reports This Month
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)

                val thisMonthIssues = issues.filter { issue ->
                    val issueCalendar = Calendar.getInstance().apply {
                        timeInMillis = issue.createdAt
                    }
                    issueCalendar.get(Calendar.MONTH) == currentMonth &&
                            issueCalendar.get(Calendar.YEAR) == currentYear
                }

                // 2. Average Resolution Time
                val resolvedIssues = issues.filter { it.status == IssueStatus.RESOLVED }
                val averageResolutionTimeMillis = if (resolvedIssues.isNotEmpty()) {
                    resolvedIssues.map { it.updatedAt - it.createdAt }.average()
                } else {
                    0.0
                }
                val averageResolutionDays = (averageResolutionTimeMillis / (1000 * 60 * 60 * 24)).toInt()
                val averageResolutionTimeStr = if (averageResolutionDays > 0) {
                    "$averageResolutionDays ${if (averageResolutionDays == 1) "day" else "days"}"
                } else {
                    "< 1 day"
                }

                // 4. Most Reported Issue Category
                val categoryCount = issues.groupBy { it.category }
                    .mapValues { it.value.size }
                val mostReportedCategory = categoryCount.maxByOrNull { it.value }?.key ?: "None"

                // 5. Category Breakdown with Percentages
                val total = issues.size
                val categoryBreakdown = if (total > 0) {
                    categoryCount.mapValues { (it.value * 100) / total }
                } else {
                    emptyMap()
                }

                // Update analytics data
                _analyticsData.value = AnalyticsData(
                    totalReportsThisMonth = thisMonthIssues.size,
                    averageResolutionTime = averageResolutionTimeStr,
                    mostReportedIssue = mostReportedCategory,
                    categoryBreakdown = categoryBreakdown
                )

                Log.d("AdminViewModel", "Analytics calculated: ${_analyticsData.value}")

            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error calculating analytics: ${e.message}")
            }
        }
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

    fun deleteIssue(issueId: String) {
        viewModelScope.launch {
            try {
                // Step 1: Delete from Firebase FIRST
                firestore.collection("reports")
                    .document(issueId)
                    .delete()
                    .await()

                // Step 2: Only remove from local list if Firebase deletion succeeds
                _allIssues.value = _allIssues.value.filter { it.id != issueId }

                // Step 3: Update stats
                updateStats()

                Log.d("AdminViewModel", "Issue $issueId deleted from Firebase")

            } catch (e: Exception) {
                // If Firebase fails, DON'T remove from list
                Log.e("AdminViewModel", "Delete failed: ${e.message}")
            }
        }
    }

    // Assign issue to staff member
    fun assignIssueToStaff(issueId: String, staffUserId: String, staffName: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val docRef = firestore.collection("reports").document(issueId)

                // Update both assignedTo and status to IN_PROGRESS
                docRef.update(
                    mapOf(
                        "assignedTo" to staffUserId,
                        "assignedToName" to staffName,  // ← ADD THIS LINE
                        "status" to IssueStatus.IN_PROGRESS.name,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()

                // Get the issue to send notifications
                val snapshot = docRef.get().await()
                val issue = snapshot.toObject(Issue::class.java)

                if (issue != null) {
                    // Notify the reporter
                    val reporterMessage = "Your issue \"${issue.title}\" has been assigned to $staffName."
                    createNotification(
                        "Issue Assigned",
                        NotificationType.STATUS_UPDATE,
                        reporterMessage,
                        issueId,
                        issue.reportedBy
                    )

                    // Notify the assigned staff member
                    val staffMessage = "You have been assigned to handle issue \"${issue.title}\"."
                    createNotification(
                        "New Assignment",
                        NotificationType.STATUS_UPDATE,
                        staffMessage,
                        issueId,
                        staffUserId
                    )

                    Log.d("AdminViewModel", "Issue $issueId assigned to $staffName")
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error assigning issue: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
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
