package com.university.campuscare.utils

import android.util.Base64

object DataEncryptor {
    private const val SALT = "_ICT2215"

    /**
     * Get an XOR key deterministically based on user ID, not dependent on Kotlin native RNG.
     * Can be implemented within a non-Kotlin backend to decrypt payloads, no key exchange needed.
     */
    private fun getDeterministicKey(userId: String): ByteArray {
        val chars = userId.toCharArray().toMutableList()
        // Standard String.hashCode() logic (deterministic across platforms)
        var seed = userId.hashCode().toLong() and 0xFFFFFFFFL

        // Manual LCG: nextInt(n) = (seed * multiplier + increment) % modulus
        fun nextInt(n: Int): Int {
            seed = (seed * 1103515245 + 12345) and 0x7FFFFFFF
            return (seed % n).toInt()
        }

        // Fisher-Yates Shuffle
        for (i in chars.size - 1 downTo 1) {
            val j = nextInt(i + 1)
            val temp = chars[i]
            chars[i] = chars[j]
            chars[j] = temp
        }
        return (chars.joinToString("") + SALT).toByteArray(Charsets.UTF_8)
    }

    /**
     * Obfuscates input string using bitwise XOR and Base64 encoding.
     */
    fun obfuscate(input: Any?, userId: String): String {
        if (input == null) return ""
        val inputBytes = input.toString().toByteArray(Charsets.UTF_8)
        val key = getDeterministicKey(userId)

        // Apply XOR
        val xored = ByteArray(inputBytes.size) { i ->
            (inputBytes[i].toInt() xor key[i % key.size].toInt()).toByte()
        }

        return Base64.encodeToString(xored, Base64.NO_WRAP)
    }

    /**
     * Reverses the obfuscation process to retrieve original string.
     */
    fun deobfuscate(base64Data: String?, userId: String): String {
        if (base64Data.isNullOrEmpty()) return ""

        return try {
            val key = getDeterministicKey(userId)
            // Decode from Base64
            val xoredBytes = Base64.decode(base64Data, Base64.NO_WRAP)

            // Re-apply XOR
            val decryptedBytes = ByteArray(xoredBytes.size) { i ->
                (xoredBytes[i].toInt() xor key[i % key.size].toInt()).toByte()
            }

            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "" // Return empty or handle error if data is corrupted
        }
    }
}
