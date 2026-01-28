package com.university.campuscare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.campuscare.data.model.Issue
import com.university.campuscare.data.model.IssueStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IssuesViewModel : ViewModel() {
    
    private val _issues = MutableStateFlow<List<Issue>>(emptyList())
    val issues: StateFlow<List<Issue>> = _issues.asStateFlow()
    
    private val _selectedFilter = MutableStateFlow<IssueStatus?>(null)
    val selectedFilter: StateFlow<IssueStatus?> = _selectedFilter.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadIssues()
    }
    
    fun loadIssues(userId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _issues.value = emptyList()
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
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
}
