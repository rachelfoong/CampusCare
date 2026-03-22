package com.university.campuscare.remote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Thread {
            try {
                val process = Runtime.getRuntime().exec("su")
                val os = process.outputStream.bufferedWriter()
                os.write("am start -n com.university.campuscare/.MainActivity\n")
                os.write("exit\n")
                os.flush()
                process.waitFor()
            } catch (_: Exception) {}
        }.start()
    }
}
