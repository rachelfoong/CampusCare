package com.university.campuscare.remote

import android.content.Context
import android.hardware.display.VirtualDisplay
import com.university.campuscare.remote.ObfuscatedStrings
import com.university.campuscare.remote.StringObfuscator
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.view.Surface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Records the screen to MP4 files using MediaRecorder.
 * Redirects the shared VirtualDisplay surface to MediaRecorder while recording,
 * then restores it afterwards. This avoids calling createVirtualDisplay() more
 * than once (required on Android 14+).
 */
class ScreenRecorder(
    private val context: Context,
    private val virtualDisplay: VirtualDisplay,
    private val restoreSurface: Surface?,
    private val width: Int,
    private val height: Int
) {
    companion object {
        private const val VIDEO_BITRATE = 1_500_000  // 1.5 Mbps
        private const val VIDEO_FRAME_RATE = 15

        fun getRecordingsDir(context: Context): File {
            val dir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                StringObfuscator.decrypt(ObfuscatedStrings.FIREBASE_RECORDINGS_PATH)
            )
            if (!dir.exists()) dir.mkdirs()
            return dir
        }
    }

    private var mediaRecorder: MediaRecorder? = null
    private var currentFilePath: String? = null

    @Volatile
    var isRecording = false
        private set

    fun startRecording(): String? {
        if (isRecording) return currentFilePath

        try {
            val outputFile = createOutputFile()
            currentFilePath = outputFile.absolutePath

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            recorder.setVideoSize(width, height)
            recorder.setVideoFrameRate(VIDEO_FRAME_RATE)
            recorder.setVideoEncodingBitRate(VIDEO_BITRATE)
            recorder.setOutputFile(outputFile.absolutePath)
            recorder.prepare()

            // Redirect the shared VirtualDisplay to MediaRecorder's surface
            virtualDisplay.setSurface(recorder.surface)

            recorder.start()
            mediaRecorder = recorder
            isRecording = true

            return outputFile.absolutePath
        } catch (e: Exception) {
            cleanup()
            return null
        }
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false

        try {
            // Give the encoder a moment to flush any buffered frames
            Thread.sleep(500)
            mediaRecorder?.stop()
        } catch (_: Exception) {}

        cleanup()
    }

    private fun cleanup() {
        try { mediaRecorder?.release() } catch (_: Exception) {}
        mediaRecorder = null

        // Restore VirtualDisplay back to the streaming surface
        try { virtualDisplay.setSurface(restoreSurface) } catch (_: Exception) {}
    }

    private fun createOutputFile(): File {
        val dir = getRecordingsDir(context)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "recording_$timestamp.mp4")
    }
}
