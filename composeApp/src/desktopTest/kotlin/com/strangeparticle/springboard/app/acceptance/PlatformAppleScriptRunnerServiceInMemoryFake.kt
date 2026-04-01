package com.strangeparticle.springboard.app.acceptance

import com.strangeparticle.springboard.app.platform.PlatformAppleScriptRunnerService
import com.strangeparticle.springboard.app.platform.ScriptRunResult

class PlatformAppleScriptRunnerServiceInMemoryFake(
    var result: ScriptRunResult = ScriptRunResult(exitCode = 0, stdout = "", stderr = ""),
    var exception: Exception? = null,
) : PlatformAppleScriptRunnerService {

    val scriptsRun: MutableList<String> = mutableListOf()

    override fun runAppleScriptFile(resourcePath: String): ScriptRunResult {
        scriptsRun.add(resourcePath)
        exception?.let { throw it }
        return result
    }
}
