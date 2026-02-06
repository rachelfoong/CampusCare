package com.university.campuscare.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.data.model.User
import com.university.campuscare.data.repository.UserProfileRepository
import com.university.campuscare.data.repository.UserProfileRepositoryImpl
import com.university.campuscare.utils.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: UserProfileRepository = UserProfileRepositoryImpl(
        FirebaseFirestore.getInstance()
    )

    private val _userProfile = MutableStateFlow<DataResult<User>>(DataResult.Loading)
    val userProfile: StateFlow<DataResult<User>> = _userProfile

    private val _updateProfileResult = MutableStateFlow<DataResult<Unit>>(DataResult.Idle)
    val updateProfileResult: StateFlow<DataResult<Unit>> = _updateProfileResult

    fun getUserProfile(userId: String) {
        viewModelScope.launch {
            _userProfile.value = DataResult.Loading
            _userProfile.value = repository.getUserProfile(userId)
        }
    }

    fun updateUserProfile(user: User) {
        viewModelScope.launch {
            _updateProfileResult.value = DataResult.Loading
            _updateProfileResult.value = repository.updateUserProfile(user)
        }
    }
}
