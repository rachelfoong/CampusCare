package com.university.campuscare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.campuscare.data.model.Issue
import com.university.campuscare.data.model.IssueStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminStats(
    val total: Int = 0,
    val pending: Int = 0,
    val active: Int = 0,
    val resolved: Int = 0
)

class AdminViewModel : ViewModel() {
    
    private val _allIssues = MutableStateFlow<List<Issue>>(emptyList())
    val allIssues: StateFlow<List<Issue>> = _allIssues.asStateFlow()
    
    private val _stats = MutableStateFlow(AdminStats())
    val stats: StateFlow<AdminStats> = _stats.asStateFlow()
    
    private val _selectedFilter = MutableStateFlow<IssueStatus?>(null)
    val selectedFilter: StateFlow<IssueStatus?> = _selectedFilter.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadAllIssues()
    }
    
    fun loadAllIssues() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _allIssues.value = emptyList()
                updateStats()
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }
    
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
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun acceptIssue(issueId: String) {
        viewModelScope.launch {
            try {
                _allIssues.value = _allIssues.value.map {
                    if (it.id == issueId) it.copy(status = IssueStatus.IN_PROGRESS) else it
                }
                updateStats()
            } catch (e: Exception) {
            }
        }
    }
    
    fun assignIssue(issueId: String, technicianId: String) {
        viewModelScope.launch {
            try {
                _allIssues.value = _allIssues.value.map {
                    if (it.id == issueId) it.copy(assignedTo = technicianId) else it
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun getFilteredIssues(): List<Issue> {
        var filtered = _allIssues.value
        
        _selectedFilter.value?.let { status ->
            filtered = filtered.filter { it.status == status }
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
