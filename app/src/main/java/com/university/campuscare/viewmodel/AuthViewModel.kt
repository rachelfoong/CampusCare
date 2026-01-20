package com.university.campuscare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

enum class UserRole {
    STUDENT, ADMIN
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val userRole: UserRole, val userName: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val hardcodedStudents = mapOf(
        "student@campus.edu" to "student123",
        "john@campus.edu" to "password"
    )
    
    private val hardcodedAdmins = mapOf(
        "admin@campus.edu" to "admin123"
    )

    fun login(email: String, password: String, isAdminLogin: Boolean = false) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            delay(1000)
            
            if (isAdminLogin) {
                if (hardcodedAdmins[email] == password) {
                    _authState.value = AuthState.Authenticated(
                        userRole = UserRole.ADMIN,
                        userName = "Administrator"
                    )
                } else {
                    _authState.value = AuthState.Error("Invalid admin credentials")
                }
            } else {
                if (hardcodedStudents[email] == password) {
                    val userName = email.substringBefore("@").capitalize()
                    _authState.value = AuthState.Authenticated(
                        userRole = UserRole.STUDENT,
                        userName = userName
                    )
                } else {
                    _authState.value = AuthState.Error("Invalid email or password")
                }
            }
        }
    }

    fun register(name: String, email: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            delay(1000)
            
            when {
                name.isBlank() -> {
                    _authState.value = AuthState.Error("Name cannot be empty")
                }
                email.isBlank() || !email.contains("@") -> {
                    _authState.value = AuthState.Error("Invalid email address")
                }
                password.length < 6 -> {
                    _authState.value = AuthState.Error("Password must be at least 6 characters")
                }
                password != confirmPassword -> {
                    _authState.value = AuthState.Error("Passwords do not match")
                }
                else -> {
                    _authState.value = AuthState.Authenticated(
                        userRole = UserRole.STUDENT,
                        userName = name
                    )
                }
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            delay(1500)
            
            if (email.isBlank() || !email.contains("@")) {
                _authState.value = AuthState.Error("Invalid email address")
            } else {
                _authState.value = AuthState.Idle
            }
        }
    }

    fun logout() {
        _authState.value = AuthState.Idle
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }
}
