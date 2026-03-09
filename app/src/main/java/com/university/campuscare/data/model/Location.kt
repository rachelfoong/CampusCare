package com.university.campuscare.data.model

data class Location(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
