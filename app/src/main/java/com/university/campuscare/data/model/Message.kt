package com.university.campuscare.data.model

import com.google.firebase.firestore.PropertyName

data class Message(
    @get:PropertyName("id") @field:PropertyName("id") val id: String = "",
    @get:PropertyName("issueId") @field:PropertyName("issueId") val issueId: String = "",
    @get:PropertyName("senderId") @field:PropertyName("senderId") val senderId: String = "",
    @get:PropertyName("senderName") @field:PropertyName("senderName") val senderName: String = "",
    @get:PropertyName("message") @field:PropertyName("message") val message: String = "",
    @get:PropertyName("timestamp") @field:PropertyName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("isFromAdmin") @field:PropertyName("isFromAdmin") val isFromAdmin: Boolean = false
)
