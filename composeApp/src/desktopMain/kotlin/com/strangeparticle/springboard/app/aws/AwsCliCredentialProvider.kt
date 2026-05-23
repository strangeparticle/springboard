package com.strangeparticle.springboard.app.aws

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.seconds

/**
 * Resolves AWS credentials by shelling out to the AWS CLI:
 *
 *   - `aws configure export-credentials --profile <p>` yields the standard
 *     process-credentials JSON shape (AccessKeyId / SecretAccessKey /
 *     SessionToken / Expiration). This handles SSO sessions, role assumption,
 *     and static profiles uniformly. The `--format process-credentials` flag
 *     is the default output and is omitted here because older but still
 *     current CLI v2 releases (e.g. v2.32.19) reject the flag as an invalid
 *     choice.
 *   - `aws configure get region --profile <p>` yields the region as plain text.
 *
 * Credentials are cached per profile in memory until 60 s before their declared
 * Expiration, at which point the next [resolve] re-shells. Static credentials
 * (no Expiration field) are cached indefinitely for the process lifetime.
 *
 * On failure, [AwsCredentialResult.Failed.message] carries the underlying
 * cause text (CLI stderr for non-zero exits, "AWS CLI not found" when the
 * binary is missing, a parse-error summary when the output is malformed)
 * so the caller can surface it to the user instead of collapsing every
 * failure into a generic message.
 */
class AwsCliCredentialProvider internal constructor(
    private val processRunner: AwsCliProcessRunner,
    private val clock: () -> Instant,
) : AwsCredentialProvider {

    constructor() : this(AwsCliProcessRunnerDefaultImpl(), { Clock.System.now() })

    private val cache = mutableMapOf<String, AwsCredentials>()
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun resolve(profile: String): AwsCredentialResult = mutex.withLock {
        val cached = cache[profile]
        if (cached != null && isFresh(cached)) return AwsCredentialResult.Success(cached)
        val resolved = resolveFromCli(profile)
        if (resolved is AwsCredentialResult.Success) {
            cache[profile] = resolved.credentials
        }
        return resolved
    }

    override fun invalidate(profile: String) {
        cache.remove(profile)
    }

    private fun isFresh(credentials: AwsCredentials): Boolean {
        val expiration = credentials.expiration ?: return true
        return expiration > clock().plus(EXPIRY_SAFETY_MARGIN)
    }

    private suspend fun resolveFromCli(profile: String): AwsCredentialResult {
        val credentialsStdout = when (val result = processRunner.run(
            args = listOf("configure", "export-credentials", "--profile", profile),
            timeoutSeconds = CLI_TIMEOUT_SECONDS,
        )) {
            is AwsCliRunResult.Success -> result.stdout
            is AwsCliRunResult.Failed -> return AwsCredentialResult.Failed(
                "`aws configure export-credentials` exited ${result.exitCode}: ${result.stderr.trim().ifEmpty { "(no stderr)" }}"
            )
            AwsCliRunResult.Unavailable -> return AwsCredentialResult.Failed(
                "AWS CLI not found. Install it and ensure it is on PATH."
            )
            AwsCliRunResult.TimedOut -> return AwsCredentialResult.Failed(
                "`aws configure export-credentials` timed out after $CLI_TIMEOUT_SECONDS seconds."
            )
        }

        val regionStdout = when (val result = processRunner.run(
            args = listOf("configure", "get", "region", "--profile", profile),
            timeoutSeconds = CLI_TIMEOUT_SECONDS,
        )) {
            is AwsCliRunResult.Success -> result.stdout
            is AwsCliRunResult.Failed -> return AwsCredentialResult.Failed(
                "`aws configure get region` exited ${result.exitCode}: ${result.stderr.trim().ifEmpty { "(no stderr)" }}"
            )
            AwsCliRunResult.Unavailable -> return AwsCredentialResult.Failed(
                "AWS CLI not found. Install it and ensure it is on PATH."
            )
            AwsCliRunResult.TimedOut -> return AwsCredentialResult.Failed(
                "`aws configure get region` timed out after $CLI_TIMEOUT_SECONDS seconds."
            )
        }

        val region = regionStdout.trim()
        if (region.isEmpty()) {
            return AwsCredentialResult.Failed("Profile '$profile' has no region configured.")
        }

        return try {
            val parsed = json.parseToJsonElement(credentialsStdout) as? JsonObject
                ?: return AwsCredentialResult.Failed("export-credentials output was not a JSON object.")
            val accessKeyId = parsed["AccessKeyId"]?.jsonPrimitive?.content
                ?: return AwsCredentialResult.Failed("export-credentials output is missing AccessKeyId.")
            val secretAccessKey = parsed["SecretAccessKey"]?.jsonPrimitive?.content
                ?: return AwsCredentialResult.Failed("export-credentials output is missing SecretAccessKey.")
            val sessionToken = parsed["SessionToken"]?.jsonPrimitive?.content
            val expiration = parsed["Expiration"]?.jsonPrimitive?.content?.let { Instant.parse(it) }
            AwsCredentialResult.Success(
                AwsCredentials(
                    accessKeyId = accessKeyId,
                    secretAccessKey = secretAccessKey,
                    sessionToken = sessionToken,
                    region = region,
                    expiration = expiration,
                )
            )
        } catch (e: Exception) {
            AwsCredentialResult.Failed("Failed to parse export-credentials output: ${e.message ?: e::class.simpleName}")
        }
    }

    private companion object {
        const val CLI_TIMEOUT_SECONDS = 5L
        val EXPIRY_SAFETY_MARGIN = 60.seconds
    }
}
