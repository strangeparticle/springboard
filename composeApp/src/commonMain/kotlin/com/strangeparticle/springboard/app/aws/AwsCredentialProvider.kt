package com.strangeparticle.springboard.app.aws

/**
 * Resolves AWS credentials for a named CLI profile. Implementations are
 * platform-specific because credential discovery requires shelling out to the
 * AWS CLI on desktop and has no in-browser equivalent on WASM.
 *
 * Returns null when the underlying resolver cannot produce credentials for any
 * reason (missing profile, expired SSO session, AWS CLI not on PATH, parse
 * failure). Callers surface a user-facing message instructing the user to run
 * `aws sso login`.
 */
interface AwsCredentialProvider {
    suspend fun resolve(profile: String): AwsCredentials?

    /**
     * Drops any cached entry for [profile] so the next [resolve] re-fetches
     * from the underlying source. Use after an auth failure (e.g. an S3 403
     * shortly after the user ran `aws sso login`) so the next attempt picks
     * up the freshly-issued credentials without waiting for the safety-margin
     * timer to expire.
     */
    fun invalidate(profile: String)
}
