package com.university.campuscare.data.model
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Location(
    @get:PropertyName("id") @field:PropertyName("id") val id: String = "",
    @get:PropertyName("uid") @field:PropertyName("uid") val userId: String = "",
    @get:PropertyName("n") @field:PropertyName("n") val name: String = "",
    @get:PropertyName("lt") @field:PropertyName("lt") val latitude: Double = 0.0,
    @get:PropertyName("lon") @field:PropertyName("lon") val longitude: Double = 0.0,
    @get:PropertyName("add") @field:PropertyName("add") val address: String? = null,
    @get:PropertyName("ts") @field:PropertyName("ts") val timestamp: Long = System.currentTimeMillis()
)
