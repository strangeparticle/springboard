package com.strangeparticle.springboard.app.unit.aws

import com.strangeparticle.springboard.app.aws.AwsCliCredentialProvider
import com.strangeparticle.springboard.app.aws.AwsCliProcessRunner
import com.strangeparticle.springboard.app.aws.AwsCliRunResult
import com.strangeparticle.springboard.app.aws.AwsCredentialResult
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AwsCliCredentialProviderTest {

    private class FakeRunner : AwsCliProcessRunner {
        var credentialsResult: AwsCliRunResult = AwsCliRunResult.Failed("not configured", 1)
        var regionResult: AwsCliRunResult = AwsCliRunResult.Failed("not configured", 1)
        var invocations = 0

        override suspend fun run(args: List<String>, timeoutSeconds: Long): AwsCliRunResult {
            invocations++
            return when {
                args.contains("export-credentials") -> credentialsResult
                args.contains("region") -> regionResult
                else -> AwsCliRunResult.Failed("unexpected args: $args", 1)
            }
        }
    }

    private fun provider(runner: FakeRunner, nowSupplier: () -> Instant = { Instant.parse("2026-05-22T10:00:00Z") }) =
        AwsCliCredentialProvider(runner, nowSupplier)

    private fun success(stdout: String) = AwsCliRunResult.Success(stdout)

    private fun credentials(result: AwsCredentialResult) =
        (result as? AwsCredentialResult.Success)?.credentials

    @Test
    fun `parses sso process-credentials json`() = runTest {
        val runner = FakeRunner().apply {
            credentialsResult = success(
                """
                {
                  "Version": 1,
                  "AccessKeyId": "AKIA1",
                  "SecretAccessKey": "secret",
                  "SessionToken": "token",
                  "Expiration": "2026-05-22T11:00:00Z"
                }
                """.trimIndent()
            )
            regionResult = success("us-east-1\n")
        }
        val resolved = credentials(provider(runner).resolve("dev"))
        assertEquals("AKIA1", resolved?.accessKeyId)
        assertEquals("secret", resolved?.secretAccessKey)
        assertEquals("token", resolved?.sessionToken)
        assertEquals("us-east-1", resolved?.region)
        assertEquals(Instant.parse("2026-05-22T11:00:00Z"), resolved?.expiration)
    }

    @Test
    fun `treats static credentials with no expiration as fresh forever`() = runTest {
        val runner = FakeRunner().apply {
            credentialsResult = success("""{"Version": 1, "AccessKeyId": "K", "SecretAccessKey": "S"}""")
            regionResult = success("eu-west-1")
        }
        val p = provider(runner)
        val first = credentials(p.resolve("static"))
        val second = credentials(p.resolve("static"))
        assertNull(first?.sessionToken)
        assertNull(first?.expiration)
        assertEquals(first, second)
        // Two CLI invocations for the first resolve (credentials + region), cached second resolve.
        assertEquals(2, runner.invocations)
    }

    @Test
    fun `re-shells after expiration safety margin`() = runTest {
        var nowValue = Instant.parse("2026-05-22T10:00:00Z")
        val runner = FakeRunner().apply {
            credentialsResult = success(
                """{"Version": 1, "AccessKeyId": "A1", "SecretAccessKey": "s",
                   "SessionToken": "t", "Expiration": "2026-05-22T10:00:30Z"}""",
            )
            regionResult = success("us-east-1")
        }
        val p = provider(runner) { nowValue }
        assertEquals("A1", credentials(p.resolve("p"))?.accessKeyId)
        // Advance past expiry minus safety margin → must re-shell.
        nowValue = Instant.parse("2026-05-22T10:01:00Z")
        runner.credentialsResult = success(
            """{"Version": 1, "AccessKeyId": "A2", "SecretAccessKey": "s",
               "SessionToken": "t", "Expiration": "2026-05-22T11:00:00Z"}""",
        )
        assertEquals("A2", credentials(p.resolve("p"))?.accessKeyId)
    }

    @Test
    fun `non-zero CLI exit surfaces stderr in Failed message`() = runTest {
        val runner = FakeRunner().apply {
            credentialsResult = AwsCliRunResult.Failed(
                stderr = "argument --format: Found invalid choice 'process-credentials'",
                exitCode = 252,
            )
            regionResult = success("us-east-1")
        }
        val result = provider(runner).resolve("p")
        assertTrue(result is AwsCredentialResult.Failed)
        val message = (result as AwsCredentialResult.Failed).message
        assertTrue(message.contains("exited 252"), "expected exit code in: $message")
        assertTrue(
            message.contains("Found invalid choice 'process-credentials'"),
            "expected stderr text in: $message",
        )
    }

    @Test
    fun `CLI binary unavailable surfaces an install hint`() = runTest {
        val runner = FakeRunner().apply {
            credentialsResult = AwsCliRunResult.Unavailable
            regionResult = success("us-east-1")
        }
        val result = provider(runner).resolve("p")
        assertTrue(result is AwsCredentialResult.Failed)
        assertTrue((result as AwsCredentialResult.Failed).message.contains("AWS CLI not found"))
    }

    @Test
    fun `CLI timeout surfaces a timeout message`() = runTest {
        val runner = FakeRunner().apply {
            credentialsResult = AwsCliRunResult.TimedOut
            regionResult = success("us-east-1")
        }
        val result = provider(runner).resolve("p")
        assertTrue(result is AwsCredentialResult.Failed)
        assertTrue((result as AwsCredentialResult.Failed).message.contains("timed out"))
    }

    @Test
    fun `Failed when region is blank`() = runTest {
        val runner = FakeRunner().apply {
            credentialsResult = success("""{"Version": 1, "AccessKeyId": "A", "SecretAccessKey": "s"}""")
            regionResult = success("   \n")
        }
        val result = provider(runner).resolve("p")
        assertTrue(result is AwsCredentialResult.Failed)
        assertTrue((result as AwsCredentialResult.Failed).message.contains("no region"))
    }

    @Test
    fun `Failed when credentials json is unparseable`() = runTest {
        val runner = FakeRunner().apply {
            credentialsResult = success("not json at all")
            regionResult = success("us-east-1")
        }
        val result = provider(runner).resolve("p")
        assertTrue(result is AwsCredentialResult.Failed)
    }

    @Test
    fun `invalidate forces the next resolve to re-shell`() = runTest {
        val runner = FakeRunner().apply {
            credentialsResult = success("""{"Version": 1, "AccessKeyId": "A1", "SecretAccessKey": "s"}""")
            regionResult = success("us-east-1")
        }
        val p = provider(runner)
        assertEquals("A1", credentials(p.resolve("dev"))?.accessKeyId)
        // Cached: a second resolve doesn't re-shell.
        val invocationsAfterFirst = runner.invocations
        assertEquals("A1", credentials(p.resolve("dev"))?.accessKeyId)
        assertEquals(invocationsAfterFirst, runner.invocations)

        // Invalidate then re-resolve: the CLI is called again and the new value is returned.
        p.invalidate("dev")
        runner.credentialsResult = success("""{"Version": 1, "AccessKeyId": "A2", "SecretAccessKey": "s"}""")
        assertEquals("A2", credentials(p.resolve("dev"))?.accessKeyId)
    }

    @Test
    fun `invalidate only affects the named profile`() = runTest {
        val runner = FakeRunner().apply {
            credentialsResult = success("""{"Version": 1, "AccessKeyId": "A1", "SecretAccessKey": "s"}""")
            regionResult = success("us-east-1")
        }
        val p = provider(runner)
        p.resolve("dev")
        p.resolve("prod")
        val invocationsAfterWarm = runner.invocations
        p.invalidate("dev")

        // 'prod' is still cached → no new CLI calls.
        p.resolve("prod")
        assertEquals(invocationsAfterWarm, runner.invocations)
        // 'dev' is invalidated → two new CLI calls (creds + region).
        p.resolve("dev")
        assertEquals(invocationsAfterWarm + 2, runner.invocations)
    }
}
