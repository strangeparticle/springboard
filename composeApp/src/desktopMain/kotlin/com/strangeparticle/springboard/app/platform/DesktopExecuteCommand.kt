package com.strangeparticle.springboard.app.platform

import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster

actual fun executeCommand(command: String) {
    val process = try {
        ProcessBuilder("/bin/bash", "-c", command)
            .redirectErrorStream(true)
            .start()
    } catch (e: Exception) {
        ToastBroadcaster.error("Failed to launch command: ${e.message}")
        return
    }
    Thread {
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val output = process.inputStream.bufferedReader().readText().trim()
            val msg = if (output.isNotEmpty()) output.take(200) else "exit code $exitCode"
            ToastBroadcaster.error("Command failed: $msg")
        }
    }.also { it.isDaemon = true }.start()
}
