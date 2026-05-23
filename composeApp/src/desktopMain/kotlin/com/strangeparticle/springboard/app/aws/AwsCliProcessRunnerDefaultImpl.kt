package com.strangeparticle.springboard.app.aws

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class AwsCliProcessRunnerDefaultImpl : AwsCliProcessRunner {
    override suspend fun run(args: List<String>, timeoutSeconds: Long): String? = withContext(Dispatchers.IO) {
        val command = listOf("aws") + args
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
}
