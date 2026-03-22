package com.university.campuscare.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class RecordingUploadHelper(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val STORAGE_PATH = "recordings"
        private const val COLLECTION = "recordings"
        private const val MAX_RECORDINGS_PER_DEVICE = 20
    }

    /**
     * Uploads a recording file to Firebase Storage and saves its metadata to Firestore.
     * Deletes the local file after a successful upload.
     *
     * @param file The local MP4 file to upload.
     * @param deviceId A stable identifier for the device (e.g. ANDROID_ID).
     * @param durationSeconds How long the recording is.
     * @return The Firebase Storage download URL on success, null on failure.
     */
    suspend fun uploadRecording(file: File, deviceId: String, durationSeconds: Int): String? {
        return try {
            // Ensure we have an authenticated user before writing to Firebase
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }

            // Parse recording time from filename (recording_yyyyMMdd_HHmmss.mp4)
            // so the timestamp reflects when the recording happened, not when it uploaded
            val timestamp = try {
                val name = file.nameWithoutExtension.removePrefix("recording_")
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).parse(name)?.time
                    ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
            val storagePath = "$STORAGE_PATH/$deviceId/${file.name}"
            val fileRef = storage.reference.child(storagePath)

            // Upload the file
            fileRef.putFile(android.net.Uri.fromFile(file)).await()
            val downloadUrl = fileRef.downloadUrl.await().toString()

            // Save metadata to Firestore
            val metadata = mapOf(
                "deviceId" to deviceId,
                "downloadUrl" to downloadUrl,
                "timestamp" to timestamp,
                "durationSeconds" to durationSeconds,
                "fileSizeBytes" to file.length()
            )
            firestore.collection(COLLECTION).add(metadata).await()

            // Delete local file to save device storage
            file.delete()

            // Enforce per-device recording limit
            pruneOldRecordings(deviceId)

            downloadUrl
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Deletes the oldest recordings for a device if the total exceeds MAX_RECORDINGS_PER_DEVICE.
     * Removes both the Firestore document and the Firebase Storage file.
     */
    private suspend fun pruneOldRecordings(deviceId: String) {
        try {
            val snapshot = firestore.collection(COLLECTION)
                .whereEqualTo("deviceId", deviceId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val toDelete = snapshot.documents.drop(MAX_RECORDINGS_PER_DEVICE)
            if (toDelete.isEmpty()) return

            for (doc in toDelete) {
                val downloadUrl = doc.getString("downloadUrl")
                // Delete from Storage
                if (!downloadUrl.isNullOrBlank()) {
                    try { storage.getReferenceFromUrl(downloadUrl).delete().await() } catch (_: Exception) {}
                }
                // Delete from Firestore
                doc.reference.delete().await()
            }
        } catch (_: Exception) {}
    }
}
