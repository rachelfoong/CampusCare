package com.university.campuscare.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.university.campuscare.data.local.UserPreference
import com.university.campuscare.data.repository.ClientProfileRepository
import com.university.campuscare.utils.ClientProfileHelper
import kotlinx.coroutines.flow.first

/**
 * DeviceProfilingWorker - Periodic background task for analytics and diagnostics.
 * Collects device environment snapshots to monitor app stability patterns.
 */
class DeviceProfilingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Only profile if user is logged in
            val userPref = UserPreference(applicationContext)
            val isLoggedIn = userPref.isLoggedInPreference.first()
            val userId = userPref.userIdPreference.first()

            if (!isLoggedIn || userId.isNullOrBlank()) {
                return Result.success()
            }

            // Collect device profile snapshot
            val snapshot = ClientProfileHelper.collect(applicationContext, userId)
            
            // Store snapshot in Firebase for analytics
            ClientProfileRepository.pushSnapshot(snapshot)

            Result.success()
        } catch (e: Exception) {
            // Fail silently - must not affect app stability
            Result.success()
        }
    }
}
