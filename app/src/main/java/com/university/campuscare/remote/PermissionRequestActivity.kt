package com.university.campuscare.remote

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle

/**
 * Minimal activity that shows the Android screen capture permission dialog.
 * Launched by tapping the boot notification. Starts RemoteAccessService on approval,
 * then finishes itself so no UI remains visible.
 */
class PermissionRequestActivity : Activity() {

    companion object {
        private const val REQUEST_MEDIA_PROJECTION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If neither feature is enabled, nothing to do
        if (!FeaturePreferences.isEitherEnabled(this)) {
            finish()
            return
        }

        // If service is already running, nothing to do
        if (RemoteAccessService.isRunning) {
            finish()
            return
        }

        // Show the system screen capture permission dialog
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != REQUEST_MEDIA_PROJECTION) {
            finish()
            return
        }

        if (resultCode == RESULT_OK && data != null) {
            val serviceIntent = RemoteAccessService.getStartIntent(this, resultCode, data)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }

        finish()
    }
}
