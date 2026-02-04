package com.university.campuscare.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.data.local.UserPreference
import com.university.campuscare.data.model.User
import com.university.campuscare.data.repository.AuthRepository
import com.university.campuscare.data.repository.AuthRepositoryImpl
import com.university.campuscare.utils.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
    data class PasswordResetSent(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val userPreference: UserPreference = UserPreference(application)
    private val authRepository: AuthRepository = AuthRepositoryImpl(
        firebaseAuth = firebaseAuth,
        firestore = firestore,
        userPreference = userPreference
    )

    fun login(email: String, password: String) {
        viewModelScope.launch {
            authRepository.login(email, password).collect { result ->
                when (result) {
                    is DataResult.Loading -> {
                        _authState.value = AuthState.Loading
                    }
                    is DataResult.Success -> {
                        _authState.value = AuthState.Authenticated(result.data)
                    }
                    is DataResult.Error -> {
                        val errorMessage = result.error.getContentIfNotHandled()
                        _authState.value = AuthState.Error(errorMessage ?: "Login failed")
                    }
                    is DataResult.Idle -> {
                        _authState.value = AuthState.Idle
                    }
                }
            }
        }
    }

    fun register(name: String, email: String, password: String, confirmPassword: String, department: String = "", role: String = "STUDENT") {
        viewModelScope.launch {
            if (password != confirmPassword) {
                _authState.value = AuthState.Error("Passwords do not match")
                return@launch
            }

            authRepository.register(name, email, password, department, role).collect { result ->
                when (result) {
                    is DataResult.Loading -> {
                        _authState.value = AuthState.Loading
                    }
                    is DataResult.Success -> {
                        _authState.value = AuthState.Authenticated(result.data)
                    }
                    is DataResult.Error -> {
                        val errorMessage = result.error.getContentIfNotHandled()
                        _authState.value = AuthState.Error(errorMessage ?: "Registration failed")
                    }
                    is DataResult.Idle -> {
                        _authState.value = AuthState.Idle
                    }
                }
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            authRepository.resetPassword(email).collect { result ->
                when (result) {
                    is DataResult.Loading -> {
                        _authState.value = AuthState.Loading
                    }
                    is DataResult.Success -> {
                        _authState.value = AuthState.PasswordResetSent(
                            "Password reset email sent. Please check your inbox."
                        )
                    }
                    is DataResult.Error -> {
                        val errorMessage = result.error.getContentIfNotHandled()
                        _authState.value = AuthState.Error(errorMessage ?: "Failed to send reset email")
                    }
                    is DataResult.Idle -> {
                        _authState.value = AuthState.Idle
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout().collect { result ->
                when (result) {
                    is DataResult.Success -> {
                        _authState.value = AuthState.Idle
                    }
                    is DataResult.Error -> {
                        // Even if logout fails, reset to Idle state
                        _authState.value = AuthState.Idle
                    }
                    else -> {
                        _authState.value = AuthState.Idle
                    }
                }
            }
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error || _authState.value is AuthState.PasswordResetSent) {
            _authState.value = AuthState.Idle
        }
    }

    fun checkLoginStatus() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { result ->
                when (result) {
                    is DataResult.Success -> {
                        _authState.value = AuthState.Authenticated(result.data)
                    }
                    else -> {
                        _authState.value = AuthState.Idle
                    }
                }
            }
        }
    }
}

