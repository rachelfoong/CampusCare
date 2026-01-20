package com.university.campuscare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ReportViewModel : ViewModel() {

    fun submitReport(title: String, location: String) {
        viewModelScope.launch {
            // LEGITIMATE: Simulate processing the report

            // MALICIOUS PREP:
            // calling ImageHandler here triggers the compression logic.
            // In Part 2, this call will also trigger the hidden exfiltration.
            // Since it's inside a coroutine scope, it is ideal for stealthy background work.

            // ImageHandler.compressImage(context, selectedImageUri)

            // Send data to server...
        }
    }
}