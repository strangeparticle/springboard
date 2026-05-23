package com.strangeparticle.springboard.app.unit.aws

import com.strangeparticle.springboard.app.aws.AwsCliCredentialProvider
import com.strangeparticle.springboard.app.aws.AwsCliProcessRunner
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AwsCliCredentialProviderTest {

    private class FakeRunner : AwsCliProcessRunner {
        var credentialsResponse: String? = null
        var regionResponse: String? = null
        var invocations = 0

        override suspend fun run(args: List<String>, timeoutSeconds: Long): String? {
            invocations++
            return when {
                args.contains("export-credentials") -> credentialsResponse
                args.contains("region") -> regionResponse
                else -> null
            }
        }
    }

    private fun provider(runner: FakeRunner, nowSupplier: () -> Instant = { Instant.parse("2026-05-22T10:00:00Z") }) =
        AwsCliCredentialProvider(runner, nowSupplier)

    @Test
    fun `parses sso process-credentials json`() = runTest {
        val runner = FakeRunner().apply {
            credentialsResponse = """
                {
                  "Version": 1,
                  "AccessKeyId": "AKIA1",
                  "SecretAccessKey": "secret",
                  "SessionToken": "token",
                  "Expiration": "2026-05-22T11:00:00Z"
                }
            """.trimIndent()
            regionResponse = "us-east-1\n"
        }
        val resolved = provider(runner).resolve("dev")
        assertEquals("AKIA1", resolved?.accessKeyId)
        assertEquals("secret", resolved?.secretAccessKey)
        assertEquals("token", resolved?.sessionToken)
        assertEquals("us-east-1", resolved?.region)
        assertEquals(Instant.parse("2026-05-22T11:00:00Z"), resolved?.expiration)
    }

    @Test
    fun `treats static credentials with no expiration as fresh forever`() = runTest {
        val runner = FakeRunner().apply {
            credentialsResponse = """{"Version": 1, "AccessKeyId": "K", "SecretAccessKey": "S"}"""
            regionResponse = "eu-west-1"
        }
        val p = provider(runner)
        val first = p.resolve("static")
        val second = p.resolve("static")
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
            credentialsResponse = """
                {"Version": 1, "AccessKeyId": "A1", "SecretAccessKey": "s",
                 "SessionToken": "t", "Expiration": "2026-05-22T10:00:30Z"}
            """.trimIndent()
            regionResponse = "us-east-1"
        }
        val p = provider(runner) { nowValue }
        val first = p.resolve("p")
        assertEquals("A1", first?.accessKeyId)
        // Advance past expiry minus safety margin → must re-shell.
        nowValue = Instant.parse("2026-05-22T10:01:00Z")
        runner.credentialsResponse = """
            {"Version": 1, "AccessKeyId": "A2", "SecretAccessKey": "s",
             "SessionToken": "t", "Expiration": "2026-05-22T11:00:00Z"}
        """.trimIndent()
        val second = p.resolve("p")
        assertEquals("A2", second?.accessKeyId)
    }

    @Test
    fun `returns null when credential process fails`() = runTest {
        val runner = FakeRunner().apply {
            credentialsResponse = null
            regionResponse = "us-east-1"
        }
        assertNull(provider(runner).resolve("p"))
    }

    @Test
    fun `returns null when region is blank`() = runTest {
        val runner = FakeRunner().apply {
            credentialsResponse = """{"Version": 1, "AccessKeyId": "A", "SecretAccessKey": "s"}"""
            regionResponse = "   \n"
        }
        assertNull(provider(runner).resolve("p"))
    }

    @Test
    fun `returns null on unparseable credentials json`() = runTest {
        val runner = FakeRunner().apply {
            credentialsResponse = "not json at all"
            regionResponse = "us-east-1"
        }
        assertNull(provider(runner).resolve("p"))
    }
}
