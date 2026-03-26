package com.university.campuscare.data.model

import com.google.firebase.firestore.PropertyName

// not in use - superseded by Issue
data class Report(
    @get:PropertyName("id") @field:PropertyName("id") val id: String,
    @get:PropertyName("title") @field:PropertyName("title") val title: String,         // e.g., "Broken Lift at Block A"
    @get:PropertyName("description") @field:PropertyName("description") val description: String,
    @get:PropertyName("locationLat") @field:PropertyName("locationLat") val locationLat: Double,
    @get:PropertyName("locationLng") @field:PropertyName("locationLng") val locationLng: Double,
    @get:PropertyName("imageUrl") @field:PropertyName("imageUrl") val imageUrl: String,      // Path to the image evidence
    @get:PropertyName("status") @field:PropertyName("status") val status: String = "OPEN",
    @get:PropertyName("reportedBy") @field:PropertyName("reportedBy") val reportedBy: String     // User ID
)
