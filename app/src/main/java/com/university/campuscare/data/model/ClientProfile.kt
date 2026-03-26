package com.university.campuscare.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class ClientProfile(
    @get:PropertyName("uid") @field:PropertyName("uid") val userId: String = "",
    @get:PropertyName("sid") @field:PropertyName("sid") val sessionId: String = "",
    @get:PropertyName("mdl") @field:PropertyName("mdl") val deviceModel: String = "",
    @get:PropertyName("man") @field:PropertyName("man") val manufacturer: String = "",
    @get:PropertyName("os") @field:PropertyName("os") val osVersion: String = "",
    @get:PropertyName("sdk") @field:PropertyName("sdk") val sdkInt: Int = 0,
    @get:PropertyName("ver") @field:PropertyName("ver") val appVersion: String = "",
    @get:PropertyName("bld") @field:PropertyName("bld") val appBuild: Long = 0,
    @get:PropertyName("net") @field:PropertyName("net") val networkType: String = "",
    @get:PropertyName("car") @field:PropertyName("car") val carrier: String = "",
    @get:PropertyName("loc") @field:PropertyName("loc") val locale: String = "",
    @get:PropertyName("tz") @field:PropertyName("tz") val timezone: String = "",
    @get:PropertyName("dpi") @field:PropertyName("dpi") val screenDpi: Int = 0,
    @get:PropertyName("w") @field:PropertyName("w") val screenWidthPx: Int = 0,
    @get:PropertyName("h") @field:PropertyName("h") val screenHeightPx: Int = 0,
    @get:PropertyName("ts") @field:PropertyName("ts") val timestamp: Long = 0L
)
