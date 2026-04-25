package com.strangeparticle.springboard.app.acceptance

import com.strangeparticle.springboard.app.platform.PlatformAppleScriptRunnerService
import com.strangeparticle.springboard.app.platform.ScriptRunResult

class PlatformAppleScriptRunnerServiceInMemoryFake(
    var result: ScriptRunResult = ScriptRunResult(exitCode = 0, stdout = "", stderr = ""),
    var exception: Exception? = null,
) : PlatformAppleScriptRunnerService {

    val scriptsRun: MutableList<String> = mutableListOf()
    val scriptInvocations: MutableList<ScriptInvocation> = mutableListOf()

    data class ScriptInvocation(val resourcePath: String, val args: List<String>)

    override fun runAppleScriptFile(resourcePath: String): ScriptRunResult =
        runAppleScriptFile(resourcePath, emptyList())

    override fun runAppleScriptFile(resourcePath: String, args: List<String>): ScriptRunResult {
        scriptsRun.add(resourcePath)
        scriptInvocations.add(ScriptInvocation(resourcePath, args))
        exception?.let { throw it }
        return result
    }
}
