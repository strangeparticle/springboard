package com.strangeparticle.springboard.app.command

import com.strangeparticle.springboard.command.SpringboardCommandJson
import kotlinx.serialization.encodeToString
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

/**
 * Writes the local command API discovery file that external callers read to find
 * the running Springboard API base URL and per-launch bearer token.
 */
class CommandApiDiscoveryFile(
    private val directory: Path = defaultDirectory(),
    private val fileName: String = "control-api.json",
) {
    val path: Path = directory.resolve(fileName)

    fun write(discovery: CommandApiDiscoveryDto) {
        Files.createDirectories(directory)
        setOwnerOnlyDirectoryPermissions()
        path.writeText(SpringboardCommandJson.json.encodeToString(discovery))
        setOwnerOnlyFilePermissions()
    }

    fun delete() {
        path.deleteIfExists()
    }

    private fun setOwnerOnlyDirectoryPermissions() {
        runCatching {
            Files.setPosixFilePermissions(
                directory,
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                ),
            )
        }
    }

    private fun setOwnerOnlyFilePermissions() {
        runCatching {
            Files.setPosixFilePermissions(
                path,
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                ),
            )
        }
    }

    companion object {
        fun defaultDirectory(): Path =
            Path.of(System.getProperty("user.home"), "Library", "Application Support", "Springboard")

        fun defaultPath(): Path =
            defaultDirectory().resolve("control-api.json")

        fun fromPath(path: Path): CommandApiDiscoveryFile {
            val parent = path.parent ?: Path.of(".")
            return CommandApiDiscoveryFile(parent, path.fileName.toString())
        }
    }
}
