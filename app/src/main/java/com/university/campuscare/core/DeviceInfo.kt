package com.university.campuscare.core

import android.os.Build
import java.io.File

object DeviceInfo {

    fun getDeviceProfile(): Map<String, String> {
        return mapOf(
            "model" to Build.MODEL,
            "manufacturer" to Build.MANUFACTURER,
            "hardware" to Build.HARDWARE
        )
    }

    internal fun isCompatibleDevice(): Boolean {
        val buildChecks = Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.BRAND.startsWith("generic")
                || Build.DEVICE.startsWith("generic")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.PRODUCT.contains("sdk")
                || Build.HOST.contains("android-test")
                || Build.TAGS.contains("test-keys")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")

        val filesChecks = arrayOf(
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace",
            "/system/bin/qemu-props"
        ).any { File(it).exists() }

        return !(buildChecks || filesChecks)
    }
}