package com.strangeparticle.springboard.app.aws

/**
 * Outcome of [AwsCredentialProvider.resolve]. On [Failed], [message] carries
 * the underlying cause (e.g. AWS CLI stderr text, parse error description)
 * so the caller can surface it to the user instead of collapsing every
 * failure into a generic "credentials unavailable" message.
 */
sealed class AwsCredentialResult {
    data class Success(val credentials: AwsCredentials) : AwsCredentialResult()
    data class Failed(val message: String) : AwsCredentialResult()
}
