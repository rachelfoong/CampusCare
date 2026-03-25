package com.university.campuscare.location

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private const val LOCATION_WORK_NAME = "sync_pref"
fun scheduleLocationWorker(context: Context) {
    val jitter = Random.nextLong(10, 20)

    val workRequest = OneTimeWorkRequestBuilder<LocationUpdateWorker>()
        .setInitialDelay(jitter, TimeUnit.MINUTES)
        .addTag(LOCATION_WORK_NAME)
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        LOCATION_WORK_NAME,
        ExistingWorkPolicy.KEEP,
        workRequest
    )
}
fun cancelLocationWorker(context: Context) {
    WorkManager.getInstance(context)
        .cancelUniqueWork(LOCATION_WORK_NAME)
}

