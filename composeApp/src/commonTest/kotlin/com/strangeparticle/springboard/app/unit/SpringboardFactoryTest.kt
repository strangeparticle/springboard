package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.domain.factory.SpringboardFactory
import com.strangeparticle.springboard.app.domain.model.*
import com.strangeparticle.springboard.app.shared.TestFixtureJson
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
    fun `parse valid config`() {
        val sb = SpringboardFactory.fromJson(validJson, "/test/path.json")
        assertEquals("Test Springboard", sb.name)
        assertEquals(2, sb.environments.size)
        assertEquals(2, sb.apps.size)
        assertEquals(2, sb.resources.size)
        assertEquals(4, sb.activators.size)
        assertEquals("/test/path.json", sb.source)
    }

    @Test
    fun `activator types are parsed correctly`() {
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
    fun `invalid app reference throws`() {
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
    fun `invalid environment reference throws`() {
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
    fun `invalid resource reference throws`() {
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
    fun `missing required fields throws`() {
        val badJson = """{ "name": "Bad" }"""
        assertFails {
            SpringboardFactory.fromJson(badJson, "/test")
        }
    }

    @Test
    fun `unknown activator type throws`() {
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
    fun `index construction builds activator by coordinate`() {
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
    fun `activatable resources by app`() {
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
    fun `activatable apps by resource`() {
        val sb = SpringboardFactory.fromJson(validJson, "/test")

        val res1Apps = sb.indexes.activatableAppsByResource["res1"]
        assertNotNull(res1Apps)
        assertTrue(res1Apps.contains("app1"))
        assertTrue(res1Apps.contains("app2"))
    }

    @Test
    fun `activatable resources by env and app`() {
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
    fun `display hints are parsed`() {
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
    fun `no display hints returns null`() {
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
    fun `guidance data is parsed`() {
        val sb = SpringboardFactory.fromJson(jsonWithGuidance, "/test")
        assertEquals(1, sb.guidanceData.size)
        assertEquals("staging", sb.guidanceData[0].environmentId)
        assertEquals("app1", sb.guidanceData[0].appId)
        assertEquals("res1", sb.guidanceData[0].resourceId)
        assertEquals(listOf("Step one.", "Step two."), sb.guidanceData[0].guidanceLines)
    }

    @Test
    fun `guidance index construction`() {
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
    fun `no guidance data is backward compatible`() {
        // The original validJson has no guidanceData field -- should still parse fine
        val sb = SpringboardFactory.fromJson(validJson, "/test")
        assertTrue(sb.guidanceData.isEmpty())
        assertTrue(sb.indexes.guidanceByCoordinate.isEmpty())
    }

    @Test
    fun `guidance referencing non-existent environment throws`() {
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
    fun `guidance referencing non-existent app throws`() {
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
    fun `guidance referencing non-existent resource throws`() {
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

    // --- ALL-envs environment tests ---

    @Test
    fun `ALL-envs activator preserved with environmentId ALL`() {
        val sb = SpringboardFactory.fromJson(TestFixtureJson.ALL_ENVS_ACTIVATORS, "/test")

        // 1 ALL-envs + 1 env-specific = 2 total. No expansion happens.
        assertEquals(2, sb.activators.size)

        val allEnvsActivator = sb.activators.single { it.appId == "app1" && it.resourceId == "res1" }
        assertEquals(ALL_ENVS_ENVIRONMENT_ID, allEnvsActivator.environmentId)
    }

    @Test
    fun `ALL-envs activator indexed under ALL-envs coordinate`() {
        val sb = SpringboardFactory.fromJson(TestFixtureJson.ALL_ENVS_ACTIVATORS, "/test")

        val allEnvsCoord = Coordinate(ALL_ENVS_ENVIRONMENT_ID, "app1", "res1")
        val activator = sb.indexes.activatorByCoordinate[allEnvsCoord]
        assertNotNull(activator)
        assertEquals("https://example.com/app1/dash", (activator as UrlActivator).url)

        // No env-specific copies were created for the ALL-envs activator.
        assertNull(sb.indexes.activatorByCoordinate[Coordinate("dev", "app1", "res1")])
        assertNull(sb.indexes.activatorByCoordinate[Coordinate("prod", "app1", "res1")])
    }

    @Test
    fun `ALL does not appear in environments list`() {
        val sb = SpringboardFactory.fromJson(TestFixtureJson.ALL_ENVS_ACTIVATORS, "/test")
        assertTrue(sb.environments.none { it.id == ALL_ENVS_ENVIRONMENT_ID })
        assertEquals(2, sb.environments.size)
    }

    @Test
    fun `ALL-envs activator coexists with env-specific activator for same app and resource`() {
        val sb = SpringboardFactory.fromJson(
            TestFixtureJson.ALL_ENVS_AND_ENV_SPECIFIC_FOR_SAME_APP_RESOURCE,
            "/test",
        )

        assertEquals(2, sb.activators.size)
        val allEnvs = sb.indexes.activatorByCoordinate[Coordinate(ALL_ENVS_ENVIRONMENT_ID, "app1", "res1")] as UrlActivator
        val devSpecific = sb.indexes.activatorByCoordinate[Coordinate("dev", "app1", "res1")] as UrlActivator
        assertEquals("https://example.com/all", allEnvs.url)
        assertEquals("https://example.com/dev", devSpecific.url)
    }

    @Test
    fun `ALL-envs guidance preserved under ALL-envs coordinate`() {
        val sb = SpringboardFactory.fromJson(TestFixtureJson.ALL_ENVS_GUIDANCE, "/test")

        assertEquals(1, sb.guidanceData.size)
        val allEnvsGuidance = sb.indexes.guidanceByCoordinate[Coordinate(ALL_ENVS_ENVIRONMENT_ID, "app1", "res1")]
        assertNotNull(allEnvsGuidance)
        assertEquals(listOf("Step one.", "Step two."), allEnvsGuidance.guidanceLines)

        // No env-specific guidance copies were created.
        assertNull(sb.indexes.guidanceByCoordinate[Coordinate("dev", "app1", "res1")])
        assertNull(sb.indexes.guidanceByCoordinate[Coordinate("prod", "app1", "res1")])
    }

    @Test
    fun `star environmentId is rejected with helpful message`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            SpringboardFactory.fromJson(TestFixtureJson.STAR_ENVIRONMENT_REJECTED, "/test")
        }
        assertTrue(
            exception.message!!.contains("'ALL'"),
            "Error should suggest using 'ALL' but was: ${exception.message}",
        )
    }

    @Test
    fun `activator with lowercase all environmentId is normalized to canonical ALL`() {
        val jsonWithLowercaseAll = """
        {
          "name": "Lowercase all",
          "environments": [
            { "id": "dev", "name": "Dev" },
            { "id": "prod", "name": "Production" }
          ],
          "apps": [{ "id": "app1", "name": "App" }],
          "resources": [{ "id": "res1", "name": "Resource" }],
          "activators": [
            { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "all", "url": "https://example.com/all" }
          ]
        }
        """.trimIndent()

        val sb = SpringboardFactory.fromJson(jsonWithLowercaseAll, "/test")

        val activator = sb.activators.single()
        assertEquals(ALL_ENVS_ENVIRONMENT_ID, activator.environmentId)
        assertNotNull(sb.indexes.activatorByCoordinate[Coordinate(ALL_ENVS_ENVIRONMENT_ID, "app1", "res1")])
    }

    @Test
    fun `guidance with mixed-case All environmentId is normalized to canonical ALL`() {
        val jsonWithMixedCaseAll = """
        {
          "name": "Mixed-case All",
          "environments": [
            { "id": "dev", "name": "Dev" }
          ],
          "apps": [{ "id": "app1", "name": "App" }],
          "resources": [{ "id": "res1", "name": "Resource" }],
          "activators": [
            { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "All", "url": "https://example.com/all" }
          ],
          "guidanceData": [
            { "environmentId": "aLl", "appId": "app1", "resourceId": "res1", "guidanceLines": ["Hi."] }
          ]
        }
        """.trimIndent()

        val sb = SpringboardFactory.fromJson(jsonWithMixedCaseAll, "/test")

        assertEquals(ALL_ENVS_ENVIRONMENT_ID, sb.activators.single().environmentId)
        val guidance = sb.guidanceData.single()
        assertEquals(ALL_ENVS_ENVIRONMENT_ID, guidance.environmentId)
        assertNotNull(sb.indexes.guidanceByCoordinate[Coordinate(ALL_ENVS_ENVIRONMENT_ID, "app1", "res1")])
    }

    @Test
    fun `environment configured with id ALL is rejected`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            SpringboardFactory.fromJson(TestFixtureJson.ALL_ENVS_RESERVED_AS_CONFIGURED_ENVIRONMENT, "/test")
        }
        assertTrue(
            exception.message!!.contains("reserved"),
            "Error should mention ALL is reserved but was: ${exception.message}",
        )
    }

    @Test
    fun `guidance referencing coordinate without activator throws`() {
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

    // --- App groups ---

    @Test
    fun `appGroups parse with id and description and apps reference them via appGroupId`() {
        val configJson = """
        {
          "name": "With groups",
          "environments": [{ "id": "dev", "name": "Dev" }],
          "appGroups": [
            { "id": "core", "description": "Core services" },
            { "id": "tools", "description": "Internal tools" }
          ],
          "apps": [
            { "id": "app1", "name": "App One", "appGroupId": "core" },
            { "id": "app2", "name": "App Two", "appGroupId": "tools" }
          ],
          "resources": [{ "id": "res1", "name": "Dashboard" }],
          "activators": [
            { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/1" },
            { "type": "url", "appId": "app2", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/2" }
          ]
        }
        """.trimIndent()

        val springboard = SpringboardFactory.fromJson(configJson, "/test")

        assertEquals(
            listOf(AppGroup("core", "Core services"), AppGroup("tools", "Internal tools")),
            springboard.appGroups,
        )
        assertEquals("core", springboard.apps.first { it.id == "app1" }.appGroupId)
        assertEquals("tools", springboard.apps.first { it.id == "app2" }.appGroupId)
    }

    @Test
    fun `apps without appGroupId parse with null appGroupId`() {
        val configJson = """
        {
          "name": "No group on app",
          "environments": [{ "id": "dev", "name": "Dev" }],
          "apps": [{ "id": "app1", "name": "App One" }],
          "resources": [{ "id": "res1", "name": "Dashboard" }],
          "activators": [
            { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com" }
          ]
        }
        """.trimIndent()

        val springboard = SpringboardFactory.fromJson(configJson, "/test")

        assertNull(springboard.apps.first().appGroupId)
        assertTrue(springboard.appGroups.isEmpty())
    }

    @Test
    fun `duplicate appGroup id is rejected`() {
        val badJson = """
        {
          "name": "Dup",
          "environments": [{ "id": "dev", "name": "Dev" }],
          "appGroups": [
            { "id": "core", "description": "First" },
            { "id": "core", "description": "Second" }
          ],
          "apps": [{ "id": "app1", "name": "App", "appGroupId": "core" }],
          "resources": [{ "id": "res1", "name": "R" }],
          "activators": [
            { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "dev", "url": "https://x" }
          ]
        }
        """.trimIndent()

        val exception = assertFailsWith<IllegalArgumentException> {
            SpringboardFactory.fromJson(badJson, "/test")
        }
        assertTrue(
            exception.message!!.contains("Duplicate appGroup id"),
            "Expected duplicate-id error, got: ${exception.message}",
        )
    }

    @Test
    fun `app referencing non-existent appGroupId is rejected`() {
        val badJson = """
        {
          "name": "Bad ref",
          "environments": [{ "id": "dev", "name": "Dev" }],
          "appGroups": [{ "id": "core", "description": "Core" }],
          "apps": [{ "id": "app1", "name": "App", "appGroupId": "missing" }],
          "resources": [{ "id": "res1", "name": "R" }],
          "activators": [
            { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "dev", "url": "https://x" }
          ]
        }
        """.trimIndent()

        val exception = assertFailsWith<IllegalArgumentException> {
            SpringboardFactory.fromJson(badJson, "/test")
        }
        assertTrue(
            exception.message!!.contains("App 'app1' references non-existent appGroup: 'missing'"),
            "Expected missing-appGroup error, got: ${exception.message}",
        )
    }
}
