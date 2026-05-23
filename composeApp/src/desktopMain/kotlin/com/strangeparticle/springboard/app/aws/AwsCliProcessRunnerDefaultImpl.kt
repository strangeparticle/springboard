package com.strangeparticle.springboard.app.aws

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class AwsCliProcessRunnerDefaultImpl : AwsCliProcessRunner {
    override suspend fun run(args: List<String>, timeoutSeconds: Long): AwsCliRunResult = withContext(Dispatchers.IO) {
        val command = listOf(resolveAwsCommand()) + args
        val process = try {
            ProcessBuilder(command).redirectErrorStream(false).start()
        } catch (e: IOException) {
            return@withContext AwsCliRunResult.Unavailable
        }
        val finished = try {
            process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            process.destroyForcibly()
            return@withContext AwsCliRunResult.TimedOut
        }
        if (!finished) {
            process.destroyForcibly()
            return@withContext AwsCliRunResult.TimedOut
        }
        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }
        val exitCode = process.exitValue()
        return@withContext if (exitCode == 0) {
            AwsCliRunResult.Success(stdout)
        } else {
            AwsCliRunResult.Failed(stderr = stderr, exitCode = exitCode)
        }
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
