package com.university.campuscare.viewmodel

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.data.model.Issue
import com.university.campuscare.data.model.IssueCategory
import com.university.campuscare.data.model.IssueLocation
import com.university.campuscare.data.model.IssueStatus
import com.university.campuscare.data.repository.IssuesRepositoryImpl
import com.university.campuscare.utils.DataResult
import com.university.campuscare.utils.PhotoUploadHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Used by ReportFaultScreen

// TODO FOR REPORTS:
// Take a photo from the UI (or select from device storage?)
// Upload the photo to Firebase storage
// Select location from a map in the UI

sealed class ReportState {
    object Idle : ReportState()
    object Loading : ReportState()
    object Success : ReportState()
    data class Error(val message: String) : ReportState()
}

class ReportViewModel : ViewModel() {
    
    private val _reportState = MutableStateFlow<ReportState>(ReportState.Idle)
    val reportState: StateFlow<ReportState> = _reportState.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<IssueCategory?>(null)
    val selectedCategory: StateFlow<IssueCategory?> = _selectedCategory.asStateFlow()
    
    private val _photoUri = MutableStateFlow<String?>(null)
    val photoUri: StateFlow<String?> = _photoUri.asStateFlow()

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val issuesRepository = IssuesRepositoryImpl(firestore)

    fun selectCategory(category: IssueCategory) {
        _selectedCategory.value = category
    }

    // firebase storage uri to the actual photo - upload function TODO
    fun setPhotoUri(uri: String?) {
        _photoUri.value = uri
    }

    // Create issue in Firebase
    // TODO - store location lat/long
    fun submitReport(
        context: Context,
        title: String,
        description: String,
        block: String,
        level: String,
        room: String,
        address: String?,
        latitude: Double?,
        longitude: Double?,
        userId: String,
        userName: String
    ) {
        viewModelScope.launch {
            try {
                if (_selectedCategory.value == null) {
                    _reportState.value = ReportState.Error("Please select an issue category")
                    return@launch
                }
                
                if (title.isBlank()) {
                    _reportState.value = ReportState.Error("Please provide a brief description")
                    return@launch
                }

                _reportState.value = ReportState.Loading

                // Upload photo if it exists
                var remotePhotoUrl: String? = null
                val localPhotoUri = _photoUri.value

                if (localPhotoUri != null) {
                    val uploadHelper = PhotoUploadHelper(context)
                    val result = uploadHelper.uploadPhoto(localPhotoUri.toUri(), userId)

                    if (result.isSuccess) {
                        remotePhotoUrl = result.getOrNull()
                    } else {
                        _reportState.value = ReportState.Error(result.exceptionOrNull()?.message ?: "Failed to upload photo")
                        return@launch
                    }
                }
                
                val issue = Issue(
                    category = _selectedCategory.value!!.name,
                    title = title,
                    description = description,
                    location = IssueLocation(
                        block = block,
                        level = level,
                        room = room,
                        address = address,
                        latitude = latitude,
                        longitude = longitude
                    ),
                    status = IssueStatus.PENDING,
                    reportedBy = userId,
                    reporterName = userName,
                    photoUrl = remotePhotoUrl
                )

                issuesRepository.submitIssue(issue).collect { result ->
                    when(result) {
                        is DataResult.Loading -> { /* already loading */ }
                        is DataResult.Success -> {
                            _reportState.value = ReportState.Success
                            // Reset form
                            _selectedCategory.value = null
                            _photoUri.value = null
                        }
                        is DataResult.Error -> {
                            _reportState.value = ReportState.Error(result.error.peekContent())
                        }
                        is DataResult.Idle -> {
                            _reportState.value = ReportState.Idle
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ReportViewModel", "Error in submitReport: ${e.message}")
                _reportState.value = ReportState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }
    
    fun resetState() {
        _reportState.value = ReportState.Idle
    }
}