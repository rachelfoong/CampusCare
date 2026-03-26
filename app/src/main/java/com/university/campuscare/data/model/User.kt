package com.university.campuscare.data.model

import com.google.firebase.firestore.PropertyName

data class User(
    @get:PropertyName("userId") @field:PropertyName("userId") val userId: String = "",
    @get:PropertyName("name") @field:PropertyName("name") val name: String = "",
    @get:PropertyName("email") @field:PropertyName("email") val email: String = "",
    @get:PropertyName("role") @field:PropertyName("role") val role: String = "STUDENT", // STUDENT, STAFF, ADMIN
    @get:PropertyName("department") @field:PropertyName("department") val department: String = "",
    @get:PropertyName("profilePhotoUrl") @field:PropertyName("profilePhotoUrl") val profilePhotoUrl: String = ""
)
