package com.university.campuscare.data.model

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "STUDENT", // STUDENT, STAFF, ADMIN
    val department: String = "",
    val profilePhotoUrl: String = ""
)
