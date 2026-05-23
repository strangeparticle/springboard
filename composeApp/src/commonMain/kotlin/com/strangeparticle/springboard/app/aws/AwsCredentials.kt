package com.strangeparticle.springboard.app.aws

import kotlinx.datetime.Instant

/**
 * AWS credentials resolved from a CLI profile. [sessionToken] is non-null for
 * temporary STS credentials (e.g. SSO, role assumption) and null for static
 * long-lived credentials. [expiration] is null for static credentials and
 * carries the STS expiry for temporary ones.
 */
data class AwsCredentials(
    val accessKeyId: String,
    val secretAccessKey: String,
    val sessionToken: String?,
    val region: String,
    val expiration: Instant?,
)
