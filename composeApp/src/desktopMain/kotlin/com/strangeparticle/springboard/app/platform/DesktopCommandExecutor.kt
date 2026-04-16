package com.strangeparticle.springboard.app.platform

actual fun executeCommand(command: String, onError: (String) -> Unit) {
    val process = try {
        ProcessBuilder("/bin/bash", "-c", command)
            .redirectErrorStream(true)
            .start()
    } catch (e: Exception) {
        onError("Failed to launch command: ${e.message}")
        return
    }
    Thread {
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val output = process.inputStream.bufferedReader().readText().trim()
            val msg = if (output.isNotEmpty()) output.take(200) else "exit code $exitCode"
            onError("Command failed: $msg")
        }
    }.also { it.isDaemon = true }.start()
}
