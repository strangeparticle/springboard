package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.domain.factory.SpringboardFactory
import com.strangeparticle.springboard.app.domain.model.*
import kotlin.test.*

class SpringboardFactoryTest {

    private val validJson = """
    {
      "name": "Test Springboard",
      "environments": [
        { "id": "staging", "name": "Staging" },
        { "id": "prod", "name": "Production" }
      ],
      "apps": [
        { "id": "app1", "name": "App One" },
        { "id": "app2", "name": "App Two" }
      ],
      "resources": [
        { "id": "res1", "name": "Dashboard" },
        { "id": "res2", "name": "Logs" }
      ],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "staging", "url": "https://example.com/dash" },
        { "type": "cmd", "appId": "app1", "resourceId": "res2", "environmentId": "staging", "commandTemplate": "open /tmp" },
        { "type": "urlTemplate", "appId": "app2", "resourceId": "res1", "environmentId": "staging", "urlTemplate": "https://example.com/${'$'}{environment.id}/dash" },
        { "type": "url", "appId": "app2", "resourceId": "res2", "environmentId": "prod", "url": "https://example.com/prod/logs" }
      ]
    }
    """.trimIndent()

    @Test
    fun testParseValidConfig() {
        val sb = SpringboardFactory.fromJson(validJson, "/test/path.json")
        assertEquals("Test Springboard", sb.name)
        assertEquals(2, sb.environments.size)
        assertEquals(2, sb.apps.size)
        assertEquals(2, sb.resources.size)
        assertEquals(4, sb.activators.size)
        assertEquals("/test/path.json", sb.source)
    }

    @Test
    fun testActivatorTypes() {
        val sb = SpringboardFactory.fromJson(validJson, "/test")
        val urlActivator = sb.activators.filterIsInstance<UrlActivator>()
        val cmdActivator = sb.activators.filterIsInstance<CommandActivator>()
        val templateActivator = sb.activators.filterIsInstance<UrlTemplateActivator>()

        assertEquals(2, urlActivator.size)
        assertEquals(1, cmdActivator.size)
        assertEquals(1, templateActivator.size)
        assertEquals("https://example.com/dash", urlActivator.first().url)
        assertEquals("open /tmp", cmdActivator.first().commandTemplate)
    }

    @Test
    fun testInvalidAppReference() {
        val badJson = """
        {
          "name": "Bad",
          "environments": [{ "id": "e1", "name": "E1" }],
          "apps": [{ "id": "a1", "name": "A1" }],
          "resources": [{ "id": "r1", "name": "R1" }],
          "activators": [
            { "type": "url", "appId": "nonexistent", "resourceId": "r1", "environmentId": "e1", "url": "https://x.com" }
          ]
        }
        """.trimIndent()
        assertFailsWith<IllegalArgumentException> {
            SpringboardFactory.fromJson(badJson, "/test")
        }
    }

    @Test
    fun testInvalidEnvironmentReference() {
        val badJson = """
        {
          "name": "Bad",
          "environments": [{ "id": "e1", "name": "E1" }],
          "apps": [{ "id": "a1", "name": "A1" }],
          "resources": [{ "id": "r1", "name": "R1" }],
          "activators": [
            { "type": "url", "appId": "a1", "resourceId": "r1", "environmentId": "nonexistent", "url": "https://x.com" }
          ]
        }
        """.trimIndent()
        assertFailsWith<IllegalArgumentException> {
            SpringboardFactory.fromJson(badJson, "/test")
        }
    }

    @Test
    fun testInvalidResourceReference() {
        val badJson = """
        {
          "name": "Bad",
          "environments": [{ "id": "e1", "name": "E1" }],
          "apps": [{ "id": "a1", "name": "A1" }],
          "resources": [{ "id": "r1", "name": "R1" }],
          "activators": [
            { "type": "url", "appId": "a1", "resourceId": "nonexistent", "environmentId": "e1", "url": "https://x.com" }
          ]
        }
        """.trimIndent()
        assertFailsWith<IllegalArgumentException> {
            SpringboardFactory.fromJson(badJson, "/test")
        }
    }

    @Test
    fun testMissingRequiredFields() {
        val badJson = """{ "name": "Bad" }"""
        assertFails {
            SpringboardFactory.fromJson(badJson, "/test")
        }
    }

    @Test
    fun testUnknownActivatorType() {
        val badJson = """
        {
          "name": "Bad",
          "environments": [{ "id": "e1", "name": "E1" }],
          "apps": [{ "id": "a1", "name": "A1" }],
          "resources": [{ "id": "r1", "name": "R1" }],
          "activators": [
            { "type": "unknown", "appId": "a1", "resourceId": "r1", "environmentId": "e1" }
          ]
        }
        """.trimIndent()
        assertFails {
            SpringboardFactory.fromJson(badJson, "/test")
        }
    }

    @Test
    fun testIndexConstruction() {
        val sb = SpringboardFactory.fromJson(validJson, "/test")

        // Activator by coordinate
        val coord = Coordinate("staging", "app1", "res1")
        val activator = sb.indexes.activatorByCoordinate[coord]
        assertNotNull(activator)
        assertIs<UrlActivator>(activator)

        // Missing coordinate
        val missing = Coordinate("prod", "app1", "res1")
        assertNull(sb.indexes.activatorByCoordinate[missing])
    }

    @Test
    fun testActivatableResourcesByApp() {
        val sb = SpringboardFactory.fromJson(validJson, "/test")

        val app1Resources = sb.indexes.activatableResourcesByApp["app1"]
        assertNotNull(app1Resources)
        assertTrue(app1Resources.contains("res1"))
        assertTrue(app1Resources.contains("res2"))

        val app2Resources = sb.indexes.activatableResourcesByApp["app2"]
        assertNotNull(app2Resources)
        assertTrue(app2Resources.contains("res1"))
        assertTrue(app2Resources.contains("res2"))
    }

    @Test
    fun testActivatableAppsByResource() {
        val sb = SpringboardFactory.fromJson(validJson, "/test")

        val res1Apps = sb.indexes.activatableAppsByResource["res1"]
        assertNotNull(res1Apps)
        assertTrue(res1Apps.contains("app1"))
        assertTrue(res1Apps.contains("app2"))
    }

    @Test
    fun testActivatableResourcesByEnvApp() {
        val sb = SpringboardFactory.fromJson(validJson, "/test")

        val stagingApp1 = sb.indexes.activatableResourcesByEnvApp["staging" to "app1"]
        assertNotNull(stagingApp1)
        assertEquals(setOf("res1", "res2"), stagingApp1)

        // app2 in prod only has res2
        val prodApp2 = sb.indexes.activatableResourcesByEnvApp["prod" to "app2"]
        assertNotNull(prodApp2)
        assertEquals(setOf("res2"), prodApp2)

        // app1 in prod has no activators
        val prodApp1 = sb.indexes.activatableResourcesByEnvApp["prod" to "app1"]
        assertNull(prodApp1)
    }

    @Test
    fun testDisplayHints() {
        val jsonWithHints = """
        {
          "name": "With Hints",
          "environments": [{ "id": "e1", "name": "E1" }],
          "apps": [{ "id": "a1", "name": "A1" }],
          "resources": [{ "id": "r1", "name": "R1" }],
          "activators": [],
          "displayHints": { "width": 800, "height": 600 }
        }
        """.trimIndent()
        val sb = SpringboardFactory.fromJson(jsonWithHints, "/test")
        assertNotNull(sb.displayHints)
        assertEquals(800, sb.displayHints!!.width)
        assertEquals(600, sb.displayHints!!.height)
    }

    @Test
    fun testNoDisplayHints() {
        val sb = SpringboardFactory.fromJson(validJson, "/test")
        assertNull(sb.displayHints)
    }

    // --- Guidance data tests ---

    private val jsonWithGuidance = """
    {
      "name": "With Guidance",
      "environments": [
        { "id": "staging", "name": "Staging" },
        { "id": "prod", "name": "Production" }
      ],
      "apps": [
        { "id": "app1", "name": "App One" }
      ],
      "resources": [
        { "id": "res1", "name": "Dashboard" },
        { "id": "res2", "name": "Logs" }
      ],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "staging", "url": "https://example.com/dash" },
        { "type": "url", "appId": "app1", "resourceId": "res2", "environmentId": "prod", "url": "https://example.com/logs" }
      ],
      "guidanceData": [
        {
          "environmentId": "staging",
          "appId": "app1",
          "resourceId": "res1",
          "guidanceLines": ["Step one.", "Step two."]
        }
      ]
    }
    """.trimIndent()

    @Test
    fun testGuidanceDataParsed() {
        val sb = SpringboardFactory.fromJson(jsonWithGuidance, "/test")
        assertEquals(1, sb.guidanceData.size)
        assertEquals("staging", sb.guidanceData[0].environmentId)
        assertEquals("app1", sb.guidanceData[0].appId)
        assertEquals("res1", sb.guidanceData[0].resourceId)
        assertEquals(listOf("Step one.", "Step two."), sb.guidanceData[0].guidanceLines)
    }

    @Test
    fun testGuidanceIndexConstruction() {
        val sb = SpringboardFactory.fromJson(jsonWithGuidance, "/test")
        val coord = Coordinate("staging", "app1", "res1")
        val guidance = sb.indexes.guidanceByCoordinate[coord]
        assertNotNull(guidance)
        assertEquals(listOf("Step one.", "Step two."), guidance.guidanceLines)

        // Coordinate without guidance returns null
        val noGuidance = Coordinate("prod", "app1", "res2")
        assertNull(sb.indexes.guidanceByCoordinate[noGuidance])
    }

    @Test
    fun testNoGuidanceDataBackwardCompat() {
        // The original validJson has no guidanceData field -- should still parse fine
        val sb = SpringboardFactory.fromJson(validJson, "/test")
        assertTrue(sb.guidanceData.isEmpty())
        assertTrue(sb.indexes.guidanceByCoordinate.isEmpty())
    }

    @Test
    fun testGuidanceReferencesNonExistentEnvironment() {
        val badJson = """
        {
          "name": "Bad",
          "environments": [{ "id": "e1", "name": "E1" }],
          "apps": [{ "id": "a1", "name": "A1" }],
          "resources": [{ "id": "r1", "name": "R1" }],
          "activators": [
            { "type": "url", "appId": "a1", "resourceId": "r1", "environmentId": "e1", "url": "https://x.com" }
          ],
          "guidanceData": [
            { "environmentId": "nonexistent", "appId": "a1", "resourceId": "r1", "guidanceLines": ["bad"] }
          ]
        }
        """.trimIndent()
        assertFailsWith<IllegalArgumentException> {
            SpringboardFactory.fromJson(badJson, "/test")
        }
    }

    @Test
    fun testGuidanceReferencesNonExistentApp() {
        val badJson = """
        {
          "name": "Bad",
          "environments": [{ "id": "e1", "name": "E1" }],
          "apps": [{ "id": "a1", "name": "A1" }],
          "resources": [{ "id": "r1", "name": "R1" }],
          "activators": [
            { "type": "url", "appId": "a1", "resourceId": "r1", "environmentId": "e1", "url": "https://x.com" }
          ],
          "guidanceData": [
            { "environmentId": "e1", "appId": "nonexistent", "resourceId": "r1", "guidanceLines": ["bad"] }
          ]
        }
        """.trimIndent()
        assertFailsWith<IllegalArgumentException> {
            SpringboardFactory.fromJson(badJson, "/test")
        }
    }

    @Test
    fun testGuidanceReferencesNonExistentResource() {
        val badJson = """
        {
          "name": "Bad",
          "environments": [{ "id": "e1", "name": "E1" }],
          "apps": [{ "id": "a1", "name": "A1" }],
          "resources": [{ "id": "r1", "name": "R1" }],
          "activators": [
            { "type": "url", "appId": "a1", "resourceId": "r1", "environmentId": "e1", "url": "https://x.com" }
          ],
          "guidanceData": [
            { "environmentId": "e1", "appId": "a1", "resourceId": "nonexistent", "guidanceLines": ["bad"] }
          ]
        }
        """.trimIndent()
        assertFailsWith<IllegalArgumentException> {
            SpringboardFactory.fromJson(badJson, "/test")
        }
    }

    @Test
    fun testGuidanceReferencesCoordinateWithoutActivator() {
        val badJson = """
        {
          "name": "Bad",
          "environments": [{ "id": "e1", "name": "E1" }, { "id": "e2", "name": "E2" }],
          "apps": [{ "id": "a1", "name": "A1" }],
          "resources": [{ "id": "r1", "name": "R1" }],
          "activators": [
            { "type": "url", "appId": "a1", "resourceId": "r1", "environmentId": "e1", "url": "https://x.com" }
          ],
          "guidanceData": [
            { "environmentId": "e2", "appId": "a1", "resourceId": "r1", "guidanceLines": ["no activator here"] }
          ]
        }
        """.trimIndent()
        assertFailsWith<IllegalArgumentException> {
            SpringboardFactory.fromJson(badJson, "/test")
        }
    }
}
