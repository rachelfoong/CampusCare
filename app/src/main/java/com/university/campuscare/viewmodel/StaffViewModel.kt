package com.university.campuscare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.data.model.User
import com.university.campuscare.data.repository.StaffRepository
import com.university.campuscare.data.repository.StaffRepositoryImpl
import com.university.campuscare.utils.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StaffViewModel : ViewModel() {

    private val staffRepository: StaffRepository = StaffRepositoryImpl(FirebaseFirestore.getInstance())

    private val _staffList = MutableStateFlow<List<User>>(emptyList())
    val staffList: StateFlow<List<User>> = _staffList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadStaff()
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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getFilteredStaff(): List<User> {
        val query = _searchQuery.value
        return if (query.isBlank()) {
            _staffList.value
        } else {
            _staffList.value.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.department.contains(query, ignoreCase = true)
            }
        }
    }
}
