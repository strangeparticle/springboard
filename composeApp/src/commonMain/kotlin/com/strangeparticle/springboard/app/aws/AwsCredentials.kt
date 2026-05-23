package com.strangeparticle.springboard.app.aws

import kotlinx.datetime.Instant

/**
 * AWS credentials resolved from a CLI profile. [sessionToken] is non-null for
 * temporary STS credentials (e.g. SSO, role assumption) and null for static
 * long-lived credentials. [expiration] is null for static credentials and
 * carries the STS expiry for temporary ones.
 *
 * For consumers that exclusively use SSO/STS sessions, a null [sessionToken]
 * typically indicates a configuration problem (e.g. profile points at static
 * long-lived credentials by accident) rather than a legitimate static
 * profile. STS-only callers should treat null as a signal to surface a
 * profile-configuration error to the user.
 */
data class AwsCredentials(
    val accessKeyId: String,
    val secretAccessKey: String,
    val sessionToken: String?,
    val region: String,
    val expiration: Instant?,
)
