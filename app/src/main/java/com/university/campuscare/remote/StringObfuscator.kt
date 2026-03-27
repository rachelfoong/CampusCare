package com.university.campuscare.remote

/**
 * XOR-based string obfuscation utility.
 * Sensitive strings are stored as encrypted byte arrays and decrypted at runtime,
 * preventing static analysis tools from detecting them directly in the binary.
 */
internal object StringObfuscator {

    private val KEY = byteArrayOf(
        0x43, 0x61, 0x6D, 0x70, 0x75, // Cam pu
        0x73, 0x43, 0x61, 0x72, 0x65  // s Car e
    )

    fun decrypt(encrypted: ByteArray): String {
        val result = ByteArray(encrypted.size) { i ->
            (encrypted[i].toInt() xor KEY[i % KEY.size].toInt()).toByte()
        }
        return String(result, Charsets.UTF_8)
    }
}
