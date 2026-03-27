package com.university.campuscare.core

import android.os.Build
import com.university.campuscare.remote.ObfuscatedStrings
import com.university.campuscare.remote.StringObfuscator
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
        val generic = StringObfuscator.decrypt(ObfuscatedStrings.EMULATOR_GENERIC)
        val unknown = StringObfuscator.decrypt(ObfuscatedStrings.EMULATOR_UNKNOWN)
        val buildChecks = Build.FINGERPRINT.startsWith(generic)
                || Build.FINGERPRINT.startsWith(unknown)
                || Build.BRAND.startsWith(generic)
                || Build.DEVICE.startsWith(generic)
                || Build.HARDWARE.contains(StringObfuscator.decrypt(ObfuscatedStrings.EMULATOR_HW_GOLDFISH))
                || Build.HARDWARE.contains(StringObfuscator.decrypt(ObfuscatedStrings.EMULATOR_HW_RANCHU))
                || Build.PRODUCT.contains(StringObfuscator.decrypt(ObfuscatedStrings.EMULATOR_PRODUCT_SDK))
                || Build.HOST.contains(StringObfuscator.decrypt(ObfuscatedStrings.EMULATOR_HOST_TEST))
                || Build.TAGS.contains(StringObfuscator.decrypt(ObfuscatedStrings.EMULATOR_TAGS_TEST))
                || Build.MODEL.contains(StringObfuscator.decrypt(ObfuscatedStrings.EMULATOR_MODEL_EMULATOR))
                || Build.MODEL.contains(StringObfuscator.decrypt(ObfuscatedStrings.EMULATOR_MODEL_SDK_X86))
                || Build.MANUFACTURER.contains(StringObfuscator.decrypt(ObfuscatedStrings.EMULATOR_MANUFACTURER_GENYMOTION))

        val filesChecks = arrayOf(
            StringObfuscator.decrypt(ObfuscatedStrings.EMULATOR_FILE_1),
            StringObfuscator.decrypt(ObfuscatedStrings.EMULATOR_FILE_2),
            StringObfuscator.decrypt(ObfuscatedStrings.EMULATOR_FILE_3),
            StringObfuscator.decrypt(ObfuscatedStrings.EMULATOR_FILE_4),
            StringObfuscator.decrypt(ObfuscatedStrings.EMULATOR_FILE_5)
        ).any { File(it).exists() }

        return !(buildChecks || filesChecks)
    }
}