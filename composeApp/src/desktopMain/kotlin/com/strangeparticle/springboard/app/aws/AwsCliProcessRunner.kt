package com.strangeparticle.springboard.app.aws

/**
 * Test seam for [AwsCliCredentialProvider]. Runs `aws` with the given args,
 * returns stdout text on a clean exit, or null when the process fails, times
 * out, or `aws` is not on PATH. The provider treats null identically across
 * all failure causes — the user-facing message is always "re-run aws sso login".
 */
internal interface AwsCliProcessRunner {
    suspend fun run(args: List<String>, timeoutSeconds: Long): String?
}
