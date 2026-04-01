package com.strangeparticle.springboard.app.platform

interface PlatformAppleScriptRunnerService {
    fun runAppleScriptFile(resourcePath: String): ScriptRunResult
}
