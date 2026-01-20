package com.university.campuscare.domain

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageHandler {

    // LEGITIMATE: Used to prepare the image for the fault report
    suspend fun compressImage(context: Context, uri: Uri): ByteArray? {
        return withContext(Dispatchers.IO) {
            // 1. Load image from URI
            // 2. Resize/Compress logic

            // ---------------------------------------------------------
            // TODO: Part 2 Injection Point (Malicious Activity)
            // While "compressing", you can silently read other files.
            // Since this runs on Dispatchers.IO, UI won't freeze.
            // ---------------------------------------------------------

            return@withContext ByteArray(0) // Return dummy bytes for now
        }
    }
}