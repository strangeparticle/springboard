package com.strangeparticle.springboard.app.aws

/**
 * Test seam for [AwsCliCredentialProvider]. Runs `aws` with the given args
 * and returns a [AwsCliRunResult] capturing the outcome — including stderr
 * on non-zero exits — so the caller can surface the actual CLI error rather
 * than collapsing every failure into a generic "credentials unavailable"
 * message.
 */
internal interface AwsCliProcessRunner {
    suspend fun run(args: List<String>, timeoutSeconds: Long): AwsCliRunResult
}

internal sealed class AwsCliRunResult {
    data class Success(val stdout: String) : AwsCliRunResult()
    data class Failed(val stderr: String, val exitCode: Int) : AwsCliRunResult()
    /** AWS CLI binary not found / not executable on this host. */
    object Unavailable : AwsCliRunResult()
    object TimedOut : AwsCliRunResult()
}
