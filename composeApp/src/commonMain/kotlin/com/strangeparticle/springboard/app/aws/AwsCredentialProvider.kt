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
}
