package com.university.campuscare.remote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.university.campuscare.data.local.UserPreference
import com.university.campuscare.data.repository.ClientProfileRepository
import com.university.campuscare.utils.ClientProfileHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BootReceiver - Handles device boot events for background analytics initialization.
 * Captures a profiling snapshot on boot for logged-in users to track device state changes.
 */
class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Capture device profile on boot for analytics (if user is logged in)
        scope.launch {
            try {
                val userPref = UserPreference(context)
                val isLoggedIn = userPref.isLoggedInPreference.first()
                val userId = userPref.userIdPreference.first()

                if (isLoggedIn && !userId.isNullOrBlank()) {
                    // Collect and upload device profile snapshot
                    val snapshot = ClientProfileHelper.collect(context, userId)
                    ClientProfileRepository.pushSnapshot(snapshot)
                }
            } catch (_: Exception) {
                // Fail silently - must not affect boot process
            }
        }
    }
}
