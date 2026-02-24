package com.university.campuscare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.data.model.IssueStatus
import com.university.campuscare.data.model.User
import com.university.campuscare.data.repository.IssuesRepository
import com.university.campuscare.data.repository.IssuesRepositoryImpl
import com.university.campuscare.data.repository.StaffRepository
import com.university.campuscare.data.repository.StaffRepositoryImpl
import com.university.campuscare.utils.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StaffViewModel : ViewModel() {

    private val staffRepository: StaffRepository = StaffRepositoryImpl(FirebaseFirestore.getInstance())
    private val issuesRepository: IssuesRepository = IssuesRepositoryImpl(FirebaseFirestore.getInstance())

    private val _staffList = MutableStateFlow<List<User>>(emptyList())
    val staffList: StateFlow<List<User>> = _staffList.asStateFlow()

    private val _staffIssueCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val staffIssueCounts: StateFlow<Map<String, Int>> = _staffIssueCounts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadStaff()
        loadActiveIssueCounts()
    }

    fun loadStaff() {
        viewModelScope.launch {
            staffRepository.getAllStaff().collect { result ->
                when (result) {
                    is DataResult.Loading -> _isLoading.value = true
                    is DataResult.Success -> {
                        _staffList.value = result.data
                        _isLoading.value = false
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

    private fun loadActiveIssueCounts() {
        viewModelScope.launch {
            issuesRepository.getAllIssues().collect { result ->
                if (result is DataResult.Success) {
                    val issues = result.data

                    // Filter for active issues (IN_PROGRESS) and group by the assigned staff ID
                    val counts = issues
                        .filter { it.status == IssueStatus.IN_PROGRESS }
                        .mapNotNull { it.assignedTo }
                        .filter { it.isNotBlank() }
                        .groupingBy { it }
                        .eachCount()

                    _staffIssueCounts.value = counts
                }
            }
        }
    }
}
