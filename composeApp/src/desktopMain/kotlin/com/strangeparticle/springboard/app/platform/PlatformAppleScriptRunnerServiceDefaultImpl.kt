package com.strangeparticle.springboard.app.platform

class PlatformAppleScriptRunnerServiceDefaultImpl : PlatformAppleScriptRunnerService {
    override fun runAppleScriptFile(resourcePath: String): ScriptRunResult =
        com.strangeparticle.springboard.app.platform.runAppleScriptFile(resourcePath)
}
