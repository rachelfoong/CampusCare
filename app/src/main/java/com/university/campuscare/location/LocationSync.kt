package com.university.campuscare.location

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val LOCATION_WORK_NAME = "location_logger"
fun scheduleLocationWorker(context: Context) {


    val workRequest =
        PeriodicWorkRequestBuilder<LocationUpdateWorker>(
            15, TimeUnit.MINUTES
        ).build()


    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        LOCATION_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        workRequest
    )
}
fun cancelLocationWorker(context: Context) {
    WorkManager.getInstance(context)
        .cancelUniqueWork(LOCATION_WORK_NAME)
}

// one-time logging (test)
//fun scheduleLocationWorker(context: Context) {
//
//    val workRequest =
//        OneTimeWorkRequestBuilder<LocationUpdateWorker>()
//        .setInitialDelay(30, TimeUnit.SECONDS)
//            .build()
//
//    WorkManager.getInstance(context).enqueue(workRequest)
//}

