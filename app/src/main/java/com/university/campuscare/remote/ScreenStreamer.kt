package com.university.campuscare.remote

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import java.io.ByteArrayOutputStream
import java.util.concurrent.LinkedBlockingQueue

/**
 * Captures screen frames as JPEG bytes for live streaming to a connected PC client.
 * Uses the shared persistent ImageReader (already connected to the VirtualDisplay)
 * rather than creating its own VirtualDisplay — required on Android 14+.
 */
class ScreenStreamer(
    private val imageReader: ImageReader,
    private val frameQueue: LinkedBlockingQueue<ByteArray>
) {
    companion object {
        private const val TARGET_FPS = 12
        private const val JPEG_QUALITY = 75
    }

    private var lastFrameTime = 0L
    private val frameInterval = 1000L / TARGET_FPS
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    @Volatile
    var isActive = false
        private set

    fun start() {
        if (isActive) return
        isActive = true

        val ht = HandlerThread("ScreenStreamer").also { it.start() }
        val h = Handler(ht.looper)
        handlerThread = ht
        handler = h

        imageReader.setOnImageAvailableListener({ reader ->
            processImage(reader)
        }, h)

        // The ImageReader buffer fills up the moment the VirtualDisplay is created
        // (before any listener was attached), stalling the producer. Draining those
        // stale frames on the handler thread unblocks the VirtualDisplay so it
        // resumes sending new frames — which then trigger the listener above.
        h.post {
            repeat(5) {
                try { imageReader.acquireLatestImage()?.close() } catch (_: Exception) {}
            }
        }
    }

    fun stop() {
        isActive = false
        imageReader.setOnImageAvailableListener(null, null)
        frameQueue.clear()
        handlerThread?.quitSafely()
        handlerThread = null
        handler = null
    }

    private fun processImage(reader: ImageReader) {
        val now = System.currentTimeMillis()
        if (now - lastFrameTime < frameInterval) {
            var image: Image? = null
            try {
                image = reader.acquireLatestImage()
            } finally {
                image?.close()
            }
            return
        }

        var image: Image? = null
        try {
            image = reader.acquireLatestImage() ?: return

            val width = imageReader.width
            val height = imageReader.height
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width

            val bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            val croppedBitmap = if (rowPadding > 0) {
                Bitmap.createBitmap(bitmap, 0, 0, width, height).also {
                    bitmap.recycle()
                }
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            croppedBitmap.recycle()

            val jpegBytes = outputStream.toByteArray()

            // Drop oldest frame if queue is full
            while (!frameQueue.offer(jpegBytes)) {
                frameQueue.poll()
            }

            lastFrameTime = now
        } catch (_: Exception) {
        } finally {
            image?.close()
        }
    }
}
