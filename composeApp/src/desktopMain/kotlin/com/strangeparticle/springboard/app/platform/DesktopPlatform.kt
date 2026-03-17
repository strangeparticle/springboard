package com.strangeparticle.springboard.app.platform

import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

actual fun openUrl(url: String) {
    Desktop.getDesktop().browse(URI(url))
}

actual fun openFileDialog(currentPath: String?): String? {
    val dialog = FileDialog(null as Frame?, "Open Springboard Config", FileDialog.LOAD)
    dialog.setFilenameFilter { _, name -> name.endsWith(".json") }
    if (currentPath != null) {
        val file = File(currentPath)
        dialog.directory = file.parent
    }
    dialog.isVisible = true
    val file = dialog.file ?: return null
    val dir = dialog.directory ?: return null
    return File(dir, file).absolutePath
}

actual fun readFileContents(path: String): String? {
    val file = File(path)
    if (!file.exists()) return null
    return file.readText()
}

actual fun formatTimestamp(millis: Long): String {
    val date = Date(millis)
    val format = SimpleDateFormat("MMM d, yyyy, h:mm:ss a")
    return format.format(date)
}

actual fun getPlatformName(): String = "Desktop"

actual fun saveLocalCopyAsFileDialog(suggestedName: String): String? {
    val dialog = FileDialog(null as Frame?, "Save a Local Copy As", FileDialog.SAVE)
    dialog.file = suggestedName
    dialog.isVisible = true
    val file = dialog.file ?: return null
    val dir = dialog.directory ?: return null
    return File(dir, file).absolutePath
}

actual fun writeFileContents(path: String, contents: String): Boolean {
    return try {
        File(path).writeText(contents)
        true
    } catch (_: Exception) {
        false
    }
}

fun executeDesktopCommand(command: String) {
    ProcessBuilder("/bin/bash", "-c", command).start()
}
