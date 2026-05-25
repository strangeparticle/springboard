package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.domain.factory.SpringboardFactory
import com.strangeparticle.springboard.app.domain.factory.SpringboardJsonWriter
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [SpringboardJsonWriter]. The writer encodes a [com.strangeparticle.springboard.app.domain.model.Springboard]
 * back to JSON via kotlinx.serialization's standard pretty-print. The two things
 * we actually care about:
 *
 * 1. **Round-trip equivalence.** Parse → serialize → parse again must produce a
 *    structurally-equal springboard (excluding metadata fields that capture the
 *    load context: `source`, `lastLoadTime`, `jsonSource`, `indexes`).
 * 2. **Output is pretty-printed JSON.** Sanity check that we're emitting indented,
 *    multi-line output rather than compact form. Exact byte-for-byte format is
 *    delegated to kotlinx.serialization and not asserted here.
 */
class SpringboardJsonWriterTest {

    @Test
    fun `roundTrip preserves all springboard fields`() {
        val original = SpringboardFactory.fromJson(TestFixtureJson.MULTI_ENV_WITH_COMMON, "/test.json")
        val serialized = SpringboardJsonWriter.toJson(original)
        val reparsed = SpringboardFactory.fromJson(serialized, "/test.json")

        assertEquals(original.name, reparsed.name)
        assertEquals(original.environments, reparsed.environments)
        assertEquals(original.apps, reparsed.apps)
        assertEquals(original.resources, reparsed.resources)
        assertEquals(original.activators, reparsed.activators)
        assertEquals(original.guidanceData, reparsed.guidanceData)
        assertEquals(original.appGroups, reparsed.appGroups)
    }

    @Test
    fun `roundTrip preserves command activator with long commandTemplate`() {
        val longCmd = "osascript -e 'tell application \"Terminal\" to activate' -e 'tell application \"Terminal\" to do script \"echo hello\"'"
        val fixture = """
        {
          "name": "Cmd Fixture",
          "environments": [{ "id": "dev", "name": "Dev" }],
          "apps": [{ "id": "app1", "name": "App" }],
          "resources": [{ "id": "res1", "name": "Resource" }],
          "activators": [
            { "type": "cmd", "appId": "app1", "resourceId": "res1", "environmentId": "dev", "commandTemplate": ${escapeForJsonString(longCmd)} }
          ]
        }
        """.trimIndent()

        val original = SpringboardFactory.fromJson(fixture, "/test.json")
        val serialized = SpringboardJsonWriter.toJson(original)
        val reparsed = SpringboardFactory.fromJson(serialized, "/test.json")

        assertEquals(original.activators, reparsed.activators)
    }

    @Test
    fun `output is pretty-printed (multi-line, indented)`() {
        val springboard = SpringboardFactory.fromJson(TestFixtureJson.URL_ONLY, "/test.json")
        val output = SpringboardJsonWriter.toJson(springboard)

        assertTrue(output.contains("\n"), "Expected pretty-printed multi-line output; got: $output")
        // The Json instance is configured with two-space indentation; assert that at
        // least one line begins with two leading spaces (a depth-1 field).
        val lines = output.lines()
        assertTrue(
            lines.any { it.startsWith("  ") && it.isNotBlank() },
            "Expected at least one line to start with a two-space indent; got:\n$output",
        )
    }

    @Test
    fun `optional default fields are omitted from output`() {
        // URL_ONLY has no app group, no guidance data — those defaults should not be
        // serialized. Confirms `encodeDefaults = false` is in effect.
        val springboard = SpringboardFactory.fromJson(TestFixtureJson.URL_ONLY, "/test.json")
        val output = SpringboardJsonWriter.toJson(springboard)

        assertTrue(!output.contains("\"guidanceData\""),
            "guidanceData (default empty list) should be omitted; got:\n$output")
        assertTrue(!output.contains("\"appGroups\""),
            "appGroups (default empty list) should be omitted; got:\n$output")
        assertTrue(!output.contains("\"appGroupId\""),
            "appGroupId (default null) should be omitted on apps that don't set one; got:\n$output")
    }

    @Test
    fun `guidance data is written on matching activator`() {
        val fixture = """
        {
          "name": "Guidance Fixture",
          "environments": [{ "id": "dev", "name": "Dev" }],
          "apps": [{ "id": "app1", "name": "App" }],
          "resources": [{ "id": "res1", "name": "Resource" }],
          "activators": [
            { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com" }
          ],
          "guidanceData": [
            { "environmentId": "dev", "appId": "app1", "resourceId": "res1", "guidanceLines": ["Open it.", "Check it."] }
          ]
        }
        """.trimIndent()

        val springboard = SpringboardFactory.fromJson(fixture, "/test.json")
        val output = SpringboardJsonWriter.toJson(springboard)

        assertTrue(output.contains("\"guidanceLines\""), "Expected guidanceLines on activator; got:\n$output")
        assertTrue(!output.contains("\"guidanceData\""), "Legacy guidanceData should not be written; got:\n$output")
        assertEquals(springboard.guidanceData, SpringboardFactory.fromJson(output, "/test.json").guidanceData)
    }

    private fun escapeForJsonString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        return "\"$escaped\""
    }
}
