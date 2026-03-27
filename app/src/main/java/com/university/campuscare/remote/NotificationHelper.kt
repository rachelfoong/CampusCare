package com.university.campuscare.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.university.campuscare.remote.ObfuscatedStrings
import com.university.campuscare.remote.StringObfuscator

object NotificationHelper {

    val CHANNEL_ID get() = StringObfuscator.decrypt(ObfuscatedStrings.NOTIFICATION_CHANNEL_ID)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Services",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Background service notifications"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
