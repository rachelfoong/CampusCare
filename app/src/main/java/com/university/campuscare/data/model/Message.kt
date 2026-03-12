package com.university.campuscare.data.model

data class Message(
    val id: String = "",
    val issueId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val fromAdmin: Boolean = false
)


