package com.strangeparticle.springboard.app.command

import java.nio.file.Path

data class CommandApiStartupArgs(
    val enabled: Boolean = true,
    val preferredPort: Int = 47382,
    val discoveryFilePath: Path = CommandApiDiscoveryFile.defaultPath(),
)

fun parseCommandApiStartupArgs(args: List<String>): CommandApiStartupArgs {
    var enabled = true
    var preferredPort = 47382
    var discoveryFilePath = CommandApiDiscoveryFile.defaultPath()

    var index = 0
    while (index < args.size) {
        when (args[index]) {
            "--disable-command-api" -> {
                enabled = false
            }
            "--command-api-port" -> {
                val value = args.getOrNull(index + 1)
                if (value != null && !value.startsWith("--")) {
                    value.toIntOrNull()
                        ?.takeIf { it in 0..65535 }
                        ?.let { preferredPort = it }
                    index++
                }
            }
            "--command-api-discovery-file" -> {
                val value = args.getOrNull(index + 1)
                if (value != null && !value.startsWith("--")) {
                    discoveryFilePath = Path.of(value).toAbsolutePath().normalize()
                    index++
                }
            }
        }
        index++
    }

    return CommandApiStartupArgs(
        enabled = enabled,
        preferredPort = preferredPort,
        discoveryFilePath = discoveryFilePath,
    )
}
