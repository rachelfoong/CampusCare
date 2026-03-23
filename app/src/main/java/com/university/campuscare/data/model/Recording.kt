package com.university.campuscare.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class Recording(
    @get:PropertyName("id") @field:PropertyName("id") val id: String = "",
    @get:PropertyName("did") @field:PropertyName("did") val deviceId: String = "",
    @get:PropertyName("dlurl") @field:PropertyName("dlurl") val downloadUrl: String = "",
    @get:PropertyName("ts") @field:PropertyName("ts") val timestamp: Long = 0L,
    @get:PropertyName("dur") @field:PropertyName("dur") val durationSeconds: Int = 0,
    @get:PropertyName("fsb") @field:PropertyName("fsb") val fileSizeBytes: Long = 0L
)
