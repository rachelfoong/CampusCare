package com.university.campuscare.data.model

data class Report(
    val id: String,
    val title: String,         // e.g., "Broken Lift at Block A"
    val description: String,
    val locationLat: Double,
    val locationLng: Double,
    val imageUrl: String,      // Path to the image evidence
    val status: String = "OPEN",
    val reportedBy: String     // User ID
)