package com.university.campuscare.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.university.campuscare.data.model.Location
import com.university.campuscare.utils.DataEncryptor
import com.university.campuscare.utils.DataResult
import com.university.campuscare.utils.Event
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LocationRepository(private val firestore: FirebaseFirestore) {

    private val TAG = "LocationRepository"

    private fun mapFromFirestore(doc: Map<String, Any>, userId: String): Location {
        val rawName = doc["n"] as? String
        val rawAddr = doc["add"] as? String
        val rawLat = doc["lt"] as? String
        val rawLon = doc["lon"] as? String

        val decrypted = Location(
            userId = userId,
            id = doc["id"] as? String ?: "",
            name = DataEncryptor.deobfuscate(rawName, userId),
            address = DataEncryptor.deobfuscate(rawAddr, userId),
            latitude = DataEncryptor.deobfuscate(rawLat, userId).toDoubleOrNull() ?: 0.0,
            longitude = DataEncryptor.deobfuscate(rawLon, userId).toDoubleOrNull() ?: 0.0,
            timestamp = doc["ts"] as? Long ?: 0L
        )

        // Debug Log
        Log.d(TAG, "Deobfuscated Log for $userId:")
        Log.d(TAG, "  Raw: name=$rawName, addr=$rawAddr, lat=$rawLat, lon=$rawLon")
        Log.d(TAG, "  Mapped: name=${decrypted.name}, addr=${decrypted.address}, lat=${decrypted.latitude}, lon=${decrypted.longitude}")

        return decrypted
    }

    /**
     * Fetches the latest location logs for a specific user.
     * Since the data is stored in an obfuscated format, we map it back to the Location model
     * using the mapFromFirestore helper.
     */
    fun getLatestLocationLogs(userId: String, limit: Long = 50): Flow<DataResult<List<Location>>> = callbackFlow {
        trySend(DataResult.Loading)

        val subscription = firestore
            .collection("location_logs")
            .document(userId)
            .collection("logs")
            .orderBy("ts", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DataResult.Error(Event(error.message ?: "Failed to fetch location logs")))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val logs = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { mapFromFirestore(it, userId) }
                    }
                    trySend(DataResult.Success(logs))
                }
            }

        awaitClose { subscription.remove() }
    }

    /**
     * Saves a location log to Firestore with obfuscated fields.
     */
    suspend fun saveLocationLog(location: Location) {
        val userId = location.userId

        val logRef = firestore
            .collection("location_logs")
            .document(userId)
            .collection("logs")
            .document()

        // Map to an obfuscated structure
        val encryptedMap = hashMapOf(
            "id" to logRef.id,
            "uid" to userId, // Keep plain for security rules & indexing
            "n" to DataEncryptor.obfuscate(location.name, userId),
            "lt" to DataEncryptor.obfuscate(location.latitude, userId),
            "lon" to DataEncryptor.obfuscate(location.longitude, userId),
            "add" to DataEncryptor.obfuscate(location.address, userId),
            "ts" to location.timestamp // Keep timestamp plain for ordering/sorting
        )

        logRef.set(encryptedMap).await()
    }
}
