package com.strangeparticle.springboard.app.platform

// Reads an .applescript file from classpath resources and pipes it to osascript via stdin.
// The scripts live as standalone .applescript files in the source tree for easy dev-time
// debugging with Script Editor.
internal fun runAppleScriptFile(resourcePath: String): ScriptRunResult =
    runAppleScriptFile(resourcePath, emptyList())

// As above, but additionally forwards the given args to the script as `argv`.
// The script must declare an `on run argv ... end run` handler to receive them.
internal fun runAppleScriptFile(resourcePath: String, args: List<String>): ScriptRunResult {
    val scriptContent = checkNotNull(
        object {}.javaClass.classLoader.getResourceAsStream(resourcePath)
    ) { "Missing AppleScript resource: $resourcePath" }
        .bufferedReader().use { it.readText() }

    val command = mutableListOf("/usr/bin/osascript", "-")
    command.addAll(args)
    val process = ProcessBuilder(command)
        .redirectErrorStream(false)
        .start()
    process.outputStream.bufferedWriter().use { it.write(scriptContent) }
    val stdout = process.inputStream.bufferedReader().readText().trim()
    val stderr = process.errorStream.bufferedReader().readText().trim()
    val exitCode = process.waitFor()
    return ScriptRunResult(exitCode = exitCode, stdout = stdout, stderr = stderr)
}
