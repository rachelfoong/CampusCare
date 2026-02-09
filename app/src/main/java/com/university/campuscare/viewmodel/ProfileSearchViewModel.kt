package com.university.campuscare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.data.model.User
import com.university.campuscare.utils.DataResult
import com.university.campuscare.utils.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileSearchViewModel : ViewModel() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _allUsers = MutableStateFlow<DataResult<List<User>>>(DataResult.Loading)
    val allUsers: StateFlow<DataResult<List<User>>> = _allUsers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userProfile = MutableStateFlow<DataResult<User>>(DataResult.Idle)
    val userProfile: StateFlow<DataResult<User>> = _userProfile.asStateFlow()

    private val _isLoadingProfile = MutableStateFlow(false)
    val isLoadingProfile: StateFlow<Boolean> = _isLoadingProfile.asStateFlow()

    fun loadAllUsers() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val snapshot = firestore.collection("users").get().await()
                val users = snapshot.toObjects(User::class.java)
                _allUsers.value = DataResult.Success(users)
            } catch (e: Exception) {
                _allUsers.value = DataResult.Error(Event(e.message ?: "Failed to load users"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                _isLoadingProfile.value = true
                val document = firestore.collection("users").document(userId).get().await()
                val user = document.toObject(User::class.java)
                if (user != null) {
                    _userProfile.value = DataResult.Success(user.copy(userId = userId))
                } else {
                    _userProfile.value = DataResult.Error(Event("User not found"))
                }
            } catch (e: Exception) {
                _userProfile.value = DataResult.Error(Event(e.message ?: "Failed to load profile"))
            } finally {
                _isLoadingProfile.value = false
            }
        }
    }
}
