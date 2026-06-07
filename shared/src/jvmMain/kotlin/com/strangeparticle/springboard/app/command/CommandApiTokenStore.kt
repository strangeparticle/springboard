package com.strangeparticle.springboard.app.command

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Persists the Command API bearer token across launches so static MCP client configurations
 * (which bake in the token) survive Springboard restarts. The token is stored next to the
 * discovery file directory with owner-only permissions; unlike the discovery file, it is NOT
 * deleted on shutdown — it is the durable source of truth for the token.
 */
internal class CommandApiTokenStore(
    private val tokenFile: Path = CommandApiDiscoveryFile.defaultDirectory().resolve("command-api-token"),
) {
    fun resolveToken(rotate: Boolean = false): String {
        if (rotate) {
            tokenFile.deleteIfExists()
        }
        if (tokenFile.exists()) {
            val existing = tokenFile.readText().trim()
            if (existing.isNotEmpty()) {
                return existing
            }
        }
        val token = CommandApiTokenGenerator.generate()
        Files.createDirectories(tokenFile.parent)
        tokenFile.writeText(token)
        setOwnerOnlyPermissions()
        return token
    }

    private fun setOwnerOnlyPermissions() {
        runCatching {
            Files.setPosixFilePermissions(
                tokenFile,
                setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
            )
        }
    }
}
