package com.strangeparticle.springboard.app.aws

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class AwsCliProcessRunnerDefaultImpl : AwsCliProcessRunner {
    override suspend fun run(args: List<String>, timeoutSeconds: Long): String? = withContext(Dispatchers.IO) {
        val command = listOf(resolveAwsCommand()) + args
        val process = try {
            ProcessBuilder(command).redirectErrorStream(false).start()
        } catch (e: IOException) {
            return@withContext null
        }
        val finished = try {
            process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            process.destroyForcibly()
            return@withContext null
        }
        if (!finished) {
            process.destroyForcibly()
            return@withContext null
        }
        if (process.exitValue() != 0) {
            return@withContext null
        }
        return@withContext process.inputStream.bufferedReader().use { it.readText() }
    }

    // Packaged macOS .app bundles inherit a minimal PATH from launchd, so a bare
    // "aws" lookup fails even when the CLI is installed at one of the standard
    // Homebrew/system locations. Probe the common install paths and fall back to
    // "aws" so PATH-based lookup still works when the binary lives somewhere else.
    private fun resolveAwsCommand(): String {
        val candidates = listOf("/usr/local/bin/aws", "/opt/homebrew/bin/aws", "/usr/bin/aws")
        return candidates.firstOrNull { File(it).canExecute() } ?: "aws"
    }
}
