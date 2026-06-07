package com.strangeparticle.springboard.app.platform

/**
 * Builds the single shell line a terminal activator sends to its session: a `cd`
 * into the working directory, optionally followed by the command to run.
 *
 * The working directory is wrapped in single quotes (with embedded single quotes
 * escaped the POSIX way, `'\''`) so paths with spaces or quotes survive intact.
 * The command is forwarded verbatim — it is authored by the user and may itself
 * contain shell operators.
 */
fun buildTerminalActivatorCommandLine(workingDirectory: String, command: String?): String {
    val quotedDirectory = singleQuoteForShell(workingDirectory)
    val cd = "cd $quotedDirectory"
    return if (command.isNullOrBlank()) {
        cd
    } else {
        "$cd && $command"
    }
}

private fun singleQuoteForShell(value: String): String {
    val escaped = value.replace("'", "'\\''")
    return "'$escaped'"
}
