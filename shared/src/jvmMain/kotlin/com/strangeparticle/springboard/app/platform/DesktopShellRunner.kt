package com.strangeparticle.springboard.app.platform

data class ScriptRunResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
) {
    fun errorSummary(): String = stderr.ifBlank { stdout }.ifBlank { "exit code $exitCode" }
}

internal fun runShellCommand(vararg command: String): ScriptRunResult {
    val process = ProcessBuilder(*command)
        .redirectErrorStream(false)
        .start()
    val stdout = process.inputStream.bufferedReader().readText().trim()
    val stderr = process.errorStream.bufferedReader().readText().trim()
    val exitCode = process.waitFor()
    return ScriptRunResult(exitCode = exitCode, stdout = stdout, stderr = stderr)
}
