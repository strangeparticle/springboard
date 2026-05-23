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
 *   - `aws configure export-credentials --profile <p> --format process-credentials`
 *     yields the standard JSON shape (AccessKeyId / SecretAccessKey /
 *     SessionToken / Expiration). This handles SSO sessions, role assumption,
 *     and static profiles uniformly.
 *   - `aws configure get region --profile <p>` yields the region as plain text.
 *
 * Credentials are cached per profile in memory until 60 s before their declared
 * Expiration, at which point the next [resolve] re-shells. Static credentials
 * (no Expiration field) are cached indefinitely for the process lifetime.
 *
 * Any non-zero exit or unparseable output collapses to a null return — the
 * caller surfaces a "re-run aws sso login" message rather than leaking parse
 * details.
 */
class AwsCliCredentialProvider internal constructor(
    private val processRunner: AwsCliProcessRunner,
    private val clock: () -> Instant,
) : AwsCredentialProvider {

    constructor() : this(AwsCliProcessRunnerDefaultImpl(), { Clock.System.now() })

    private val cache = mutableMapOf<String, AwsCredentials>()
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun resolve(profile: String): AwsCredentials? = mutex.withLock {
        val cached = cache[profile]
        if (cached != null && isFresh(cached)) return cached
        val resolved = resolveFromCli(profile) ?: return null
        cache[profile] = resolved
        return resolved
    }

    override fun invalidate(profile: String) {
        cache.remove(profile)
    }

    private fun isFresh(credentials: AwsCredentials): Boolean {
        val expiration = credentials.expiration ?: return true
        return expiration > clock().plus(EXPIRY_SAFETY_MARGIN)
    }

    private suspend fun resolveFromCli(profile: String): AwsCredentials? {
        val credentialsStdout = processRunner.run(
            args = listOf("configure", "export-credentials", "--profile", profile, "--format", "process-credentials"),
            timeoutSeconds = CLI_TIMEOUT_SECONDS,
        ) ?: return null

        val regionStdout = processRunner.run(
            args = listOf("configure", "get", "region", "--profile", profile),
            timeoutSeconds = CLI_TIMEOUT_SECONDS,
        ) ?: return null

        val region = regionStdout.trim()
        if (region.isEmpty()) return null

        return try {
            val parsed = json.parseToJsonElement(credentialsStdout) as? JsonObject ?: return null
            val accessKeyId = parsed["AccessKeyId"]?.jsonPrimitive?.content ?: return null
            val secretAccessKey = parsed["SecretAccessKey"]?.jsonPrimitive?.content ?: return null
            val sessionToken = parsed["SessionToken"]?.jsonPrimitive?.content
            val expiration = parsed["Expiration"]?.jsonPrimitive?.content?.let { Instant.parse(it) }
            AwsCredentials(
                accessKeyId = accessKeyId,
                secretAccessKey = secretAccessKey,
                sessionToken = sessionToken,
                region = region,
                expiration = expiration,
            )
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val CLI_TIMEOUT_SECONDS = 5L
        val EXPIRY_SAFETY_MARGIN = 60.seconds
    }
}
