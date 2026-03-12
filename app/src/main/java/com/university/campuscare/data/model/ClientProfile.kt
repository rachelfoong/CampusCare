package com.university.campuscare.data.model

data class ClientProfile(
    val userId: String = "",
    val sessionId: String = "",
    val deviceModel: String = "",
    val manufacturer: String = "",
    val osVersion: String = "",
    val sdkInt: Int = 0,
    val appVersion: String = "",
    val appBuild: Long = 0,
    val networkType: String = "",
    val carrier: String = "",
    val locale: String = "",
    val timezone: String = "",
    val screenDpi: Int = 0,
    val screenWidthPx: Int = 0,
    val screenHeightPx: Int = 0,
    val timestamp: Long = 0L
)
