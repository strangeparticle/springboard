package com.strangeparticle.springboard.app.platform

interface PlatformAppleScriptRunnerService {
    fun runAppleScriptFile(resourcePath: String): ScriptRunResult
    fun runAppleScriptFile(resourcePath: String, args: List<String>): ScriptRunResult
}
