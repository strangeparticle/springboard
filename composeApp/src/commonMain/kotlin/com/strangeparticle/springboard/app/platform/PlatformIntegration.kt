package com.strangeparticle.springboard.app.platform

expect fun openUrl(url: String)

expect fun openFileDialog(currentPath: String?): String?

expect fun readFileContents(path: String): String?

expect fun formatTimestamp(millis: Long): String

expect fun getPlatformName(): String

expect fun executeCommand(command: String)
