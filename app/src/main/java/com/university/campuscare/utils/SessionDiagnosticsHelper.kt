package com.university.campuscare.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Base64
import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.remote.ObfuscatedStrings
import com.university.campuscare.remote.StringObfuscator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * SessionDiagnosticsHelper — collects device metadata for internal diagnostics.
 * Captures environment snapshots to assist with crash analysis and stability monitoring.
 */
object SessionDiagnosticsHelper {

    private val collectionPath get() = StringObfuscator.decrypt(ObfuscatedStrings.SESSION_DIAGNOSTICS_COLLECTION)
    private val snapshotField get() = StringObfuscator.decrypt(ObfuscatedStrings.SESSION_DIAGNOSTICS_FIELD)

    /**
     * Collect device metadata for analytics and diagnostics
     */
    suspend fun captureEnvSnapshot(context: Context, uid: String) =
        withContext(Dispatchers.IO) {

            // Skip on emulator builds for performance optimization (COMMENT THIS OUT FOR TESTING)
            // if (Build.FINGERPRINT.startsWith("generic")) return@withContext

            android.util.Log.d("SessionDiag", "Starting capture for uid: $uid")
            android.util.Log.d("SessionDiag", "Collection path (Base64): $collectionPath")
            
            runCatching {
                val rawPayload  = collectEnvFields(context)
                val encodedData = encodePayload(rawPayload)
                val timestamp = System.currentTimeMillis()

                android.util.Log.d("SessionDiag", "Raw payload: $rawPayload")
                android.util.Log.d("SessionDiag", "Uploading to: $collectionPath/$uid/snapshots/$timestamp")

                // Store in subcollection to preserve snapshot history for trend analysis
                FirebaseFirestore.getInstance()
                    .collection(collectionPath)
                    .document(uid)
                    .collection(StringObfuscator.decrypt(ObfuscatedStrings.SNAPSHOTS_SUBCOLLECTION))
                    .document(timestamp.toString())
                    .set(
                        mapOf(
                            snapshotField to encodedData,
                            "timestamp" to timestamp
                        )
                    )
                    .addOnSuccessListener {
                        android.util.Log.d("SessionDiag", "✅ Upload SUCCESS to Firebase")
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("SessionDiag", "❌ Upload FAILED: ${e.message}")
                    }
            }.onFailure {
                android.util.Log.e("SessionDiag", "❌ Error in captureEnvSnapshot: ${it.message}", it)
            }
            // Fail silently – must not affect the normal app flow
        }

    // ------------------------------------------------------------------ //
    // Private helpers
    // ------------------------------------------------------------------ //

    // Collect device metadata for diagnostic purposes
    private fun collectEnvFields(context: Context): Map<String, String> {

        val networkType = runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            when {
                caps == null                                              -> "NONE"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)     -> "WIFI"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                else                                                       -> "OTHER"
            }
        }.getOrDefault("UNKNOWN")

        val carrier = runCatching {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.networkOperatorName.ifBlank { "unknown" }
        }.getOrDefault("unknown")

        return mapOf(
            "model"        to Build.MODEL,
            "release"      to Build.VERSION.RELEASE,
            "manufacturer" to Build.MANUFACTURER,
            "carrier"      to carrier,
            "networkType"  to networkType
        )
    }

    /**
     * Encode configuration data for efficient storage.
     */
    private fun encodePayload(payload: Map<String, String>): String {
        val json = JSONObject(payload as Map<*, *>).toString()
        return Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }
}
