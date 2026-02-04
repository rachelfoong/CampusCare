package com.university.campuscare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.campuscare.data.model.Issue
import com.university.campuscare.data.model.IssueStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.data.repository.IssuesRepositoryImpl
import com.university.campuscare.utils.DataResult
import kotlinx.coroutines.Job

sealed class IssuesState {
    object Idle : IssuesState()
    object Loading : IssuesState()
    object Success : IssuesState()
    data class Error(val message: String) : IssuesState()
}

// TODO FOR ISSUES:
// Note - IssuesTab.kt is currently used for the issues tab UI, not MyIssuesScreen!
// Tap on each issue to open its detailed view
// Button for easy access to the corresponding chat
class IssuesViewModel : ViewModel() {
    private val _issuesState = MutableStateFlow<IssuesState>(IssuesState.Idle)
    val issuesState: StateFlow<IssuesState> = _issuesState.asStateFlow()

    private val _issues = MutableStateFlow<List<Issue>>(emptyList())
    val issues: StateFlow<List<Issue>> = _issues.asStateFlow()

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val issuesRepository = IssuesRepositoryImpl(firestore)

    private val _selectedFilter = MutableStateFlow<IssueStatus?>(null)
    val selectedFilter: StateFlow<IssueStatus?> = _selectedFilter.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var loadIssuesJob: Job? = null

    // Load all issues for the user
    fun loadIssues(userId: String? = null) {
        if (userId.isNullOrEmpty()) return

        loadIssuesJob?.cancel()
        loadIssuesJob = viewModelScope.launch {
            issuesRepository.getMyIssues(userId).collect { result ->
                when(result) {
                    is DataResult.Success -> {
                        _issues.value = result.data
                        _issuesState.value = IssuesState.Success
                        _isLoading.value = false
                    }
                    is DataResult.Error -> {
                        _issuesState.value = IssuesState.Error(result.error.peekContent() ?: "Failed to load issues")
                        _isLoading.value = false
                    }
                    is DataResult.Loading -> {
                        _issuesState.value = IssuesState.Loading
                        _isLoading.value = true
                    }
                    is DataResult.Idle -> {
                        _issuesState.value = IssuesState.Idle
                        _isLoading.value = false
                    }
                }
            }
        }
    }

    fun setFilter(status: IssueStatus?) {
        _selectedFilter.value = status
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun getFilteredIssues(): List<Issue> {
        var filtered = _issues.value
        
        // Apply status filter
        _selectedFilter.value?.let { status ->
            filtered = filtered.filter { it.status == status }
        }
        
        // Apply search filter
        if (_searchQuery.value.isNotBlank()) {
            filtered = filtered.filter { issue ->
                issue.title.contains(_searchQuery.value, ignoreCase = true) ||
                issue.description.contains(_searchQuery.value, ignoreCase = true)
            }
        }
        
        return filtered
    }

    // Get info of one issue by its ID (for detailed view screen)
    fun getIssueById(issueId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            issuesRepository.getIssueById(issueId).collect { result ->
                when (result) {
                    is DataResult.Success -> {
                        _issues.value = listOf(result.data)
                        _issuesState.value = IssuesState.Success
                        _isLoading.value = false
                    }

                    is DataResult.Error -> {
                        _issuesState.value =
                            IssuesState.Error(result.error.peekContent() ?: "Failed to load issue")
                        _isLoading.value = false
                    }

                    is DataResult.Loading -> {
                        _issuesState.value = IssuesState.Loading
                        _isLoading.value = true
                    }

                    is DataResult.Idle -> {
                        _issuesState.value = IssuesState.Idle
                        _isLoading.value = false
                    }
                }
            }
        }
    }
}
