package com.university.campuscare.data.model

data class Recording(
    val id: String = "",
    val deviceId: String = "",
    val downloadUrl: String = "",
    val timestamp: Long = 0L,
    val durationSeconds: Int = 0,
    val fileSizeBytes: Long = 0L
)
