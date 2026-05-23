package com.strangeparticle.springboard.app.aws

/**
 * Resolves AWS credentials for a named CLI profile. Implementations are
 * platform-specific because credential discovery requires shelling out to the
 * AWS CLI on desktop and has no in-browser equivalent on WASM.
 *
 * Returns [AwsCredentialResult.Failed] (carrying the underlying cause text)
 * when the resolver cannot produce credentials. Callers surface that text to
 * the user — often with an SSO-login hint appended — so the user sees the
 * actual failure (missing profile, unsupported CLI flag, parse error, etc.)
 * rather than a generic "credentials unavailable" message.
 */
interface AwsCredentialProvider {
    suspend fun resolve(profile: String): AwsCredentialResult

    /**
     * Drops any cached entry for [profile] so the next [resolve] re-fetches
     * from the underlying source. Use after an auth failure (e.g. an S3 403
     * shortly after the user ran `aws sso login`) so the next attempt picks
     * up the freshly-issued credentials without waiting for the safety-margin
     * timer to expire.
     */
    fun invalidate(profile: String)
}
