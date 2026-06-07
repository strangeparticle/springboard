package com.strangeparticle.springboard.app.platform

/**
 * Reads and writes S3 objects authenticated via the AWS CLI. The interface
 * lives in commonMain so view-model logic can depend on it; the impl lives in
 * desktopMain because credential resolution requires shelling out to `aws`.
 *
 * Both operations return a sealed result rather than throwing. Network/I/O
 * errors map to [S3GetResult.Failed] / [S3PutResult.Failed]; HTTP status codes
 * map to named variants so callers can render the right user-facing message
 * (or, for [S3PutResult.Conflict], open the modified-externally dialog).
 */
interface S3ContentService {
    suspend fun getObject(url: String, profile: String): S3GetResult
    suspend fun putObject(url: String, profile: String, content: String, ifMatch: String?): S3PutResult
}

sealed class S3GetResult {
    data class Success(val content: String, val etag: String?) : S3GetResult()
    data class Denied(val message: String) : S3GetResult()
    data class CredentialsUnavailable(val message: String) : S3GetResult()
    data class Failed(val message: String) : S3GetResult()
}

sealed class S3PutResult {
    data class Success(val etag: String?) : S3PutResult()
    data class Conflict(val message: String) : S3PutResult()
    data class Denied(val message: String) : S3PutResult()
    data class CredentialsUnavailable(val message: String) : S3PutResult()
    data class Failed(val message: String) : S3PutResult()
}
