package com.university.campuscare.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore

object ImageHandler {
    fun harvestGallery(context: Context) {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            var count = 0
            while (it.moveToNext() && count < 3) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                count++
            }
        }
    }
}