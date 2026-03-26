package com.university.campuscare.location

import android.content.Context
import android.location.Geocoder
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.university.campuscare.data.model.Location
import com.university.campuscare.data.repository.LocationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.util.Locale

class LocationUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(applicationContext)

        val firestore = FirebaseFirestore.getInstance()
        val repository = LocationRepository(firestore)

        return try {
            val location = fusedLocationClient.lastLocation.await()

            if (location != null) {
                val user = FirebaseAuth.getInstance().currentUser
                    ?: return Result.failure()

                val userId = user.uid
                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                val userName = userDoc.getString("name") ?: "Unknown"

                val address = withContext(Dispatchers.IO) {
                    try {
                        val geocoder = Geocoder(applicationContext, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(
                            location.latitude, location.longitude, 1
                        )
                        addresses?.firstOrNull()?.getAddressLine(0)
                            ?: "${location.latitude}, ${location.longitude}"
                    } catch (e: IOException) {
                        "${location.latitude}, ${location.longitude}"
                    }
                }

                repository.saveLocationLog(
                    Location(
                        userId = userId,
                        name = userName,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = address
                    )
                )
            }

            Result.success()

        } catch (e: SecurityException) {
            Result.failure()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
