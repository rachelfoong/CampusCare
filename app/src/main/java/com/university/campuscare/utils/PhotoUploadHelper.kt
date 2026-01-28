package com.university.campuscare.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class PhotoUploadHelper(
    private val context: Context,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    
    private val storageRef = storage.reference
    
    /**
     * Upload photo to Firebase Storage
     * @param uri Photo URI from camera or gallery
     * @param userId User ID for organizing uploads
     * @return URL of uploaded photo
     */
    suspend fun uploadPhoto(uri: Uri, userId: String): Result<String> {
        return try {
            // Compress image before upload
            val compressedFile = compressImage(uri)
            
            // Create unique filename
            val filename = "issue_photos/$userId/${UUID.randomUUID()}.jpg"
            val photoRef = storageRef.child(filename)
            
            // Upload file
            val uploadTask = photoRef.putFile(Uri.fromFile(compressedFile)).await()
            
            // Get download URL
            val downloadUrl = photoRef.downloadUrl.await()
            
            // Clean up temporary file
            compressedFile.delete()
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Compress image to reduce file size
     * Max width: 1024px
     * Quality: 80%
     */
    private fun compressImage(uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        
        // Calculate new dimensions
        val maxWidth = 1024
        val ratio = maxWidth.toFloat() / bitmap.width
        val newHeight = (bitmap.height * ratio).toInt()
        
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
        
        // Compress to JPEG
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        
        // Save to temp file
        val tempFile = File(context.cacheDir, "temp_${UUID.randomUUID()}.jpg")
        val fileOutputStream = FileOutputStream(tempFile)
        fileOutputStream.write(outputStream.toByteArray())
        fileOutputStream.close()
        
        return tempFile
    }
    
    /**
     * Delete photo from Firebase Storage
     */
    suspend fun deletePhoto(photoUrl: String): Result<Boolean> {
        return try {
            val photoRef = storage.getReferenceFromUrl(photoUrl)
            photoRef.delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
