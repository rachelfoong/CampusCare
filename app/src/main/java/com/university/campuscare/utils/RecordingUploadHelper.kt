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
     * Metadata is obfuscated using DataEncryptor before storage.
     *
     * @param file The local MP4 file to upload.
     * @param deviceId A stable identifier for the device (e.g. ANDROID_ID).
     * @param durationSeconds How long the recording is.
     * @return The Firebase Storage download URL on success, null on failure.
     */
    suspend fun uploadRecording(file: File, deviceId: String, durationSeconds: Int): String? {
        return try {
            // Ensure we have an authenticated user
            val currentUser = auth.currentUser ?: auth.signInAnonymously().await().user
            val userId = currentUser?.uid ?: return null

            // Parse recording time from filename (recording_yyyyMMdd_HHmmss.mp4)
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

            // Save metadata with minified field names and obfuscated values
            val docRef = firestore.collection(COLLECTION).document()
            val metadata = hashMapOf(
                "id" to docRef.id,
                "did" to DataEncryptor.obfuscate(deviceId, userId),
                "dlurl" to DataEncryptor.obfuscate(downloadUrl, userId),
                "ts" to timestamp,
                "dur" to durationSeconds,
                "fsb" to file.length()
            )
            docRef.set(metadata).await()

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
     * Handles deobfuscation of fields to identify and delete associated storage files.
     */
    private suspend fun pruneOldRecordings(deviceId: String) {
        val userId = auth.currentUser?.uid ?: return
        val obfuscatedDeviceId = DataEncryptor.obfuscate(deviceId, userId)

        try {
            val snapshot = firestore.collection(COLLECTION)
                .whereEqualTo("did", obfuscatedDeviceId)
                .orderBy("ts", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val toDelete = snapshot.documents.drop(MAX_RECORDINGS_PER_DEVICE)
            if (toDelete.isEmpty()) return

            for (doc in toDelete) {
                // Deobfuscate downloadUrl to find and delete the Storage object
                val obfuscatedUrl = doc.getString("dlurl")
                val downloadUrl = DataEncryptor.deobfuscate(obfuscatedUrl, userId)
                
                if (downloadUrl.isNotEmpty()) {
                    try { 
                        storage.getReferenceFromUrl(downloadUrl).delete().await() 
                    } catch (_: Exception) {}
                }
                
                // Delete the Firestore metadata record
                doc.reference.delete().await()
            }
        } catch (_: Exception) {}
    }
}
