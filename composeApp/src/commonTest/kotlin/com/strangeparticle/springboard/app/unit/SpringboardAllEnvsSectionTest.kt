package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.domain.factory.SpringboardFactory
import com.strangeparticle.springboard.app.domain.model.envSpecificResources
import com.strangeparticle.springboard.app.domain.model.hasAnyAllEnvsActivators
import com.strangeparticle.springboard.app.domain.model.allEnvsResources
import com.strangeparticle.springboard.app.domain.model.displayNameForEnvironmentId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpringboardAllEnvsSectionTest {

    private val springboardWithAllEnvsAndEnvActivators = SpringboardFactory.fromJson(
        """
        {
          "name": "Mixed",
          "environments": [
            { "id": "dev", "name": "Dev" },
            { "id": "prod", "name": "Production" }
          ],
          "apps": [{ "id": "app1", "name": "App" }],
          "resources": [
            { "id": "res1", "name": "Dashboard" },
            { "id": "res2", "name": "Logs" },
            { "id": "res3", "name": "Settings" }
          ],
          "activators": [
            { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "ALL", "url": "https://example.com/all1" },
            { "type": "url", "appId": "app1", "resourceId": "res3", "environmentId": "ALL", "url": "https://example.com/all3" },
            { "type": "url", "appId": "app1", "resourceId": "res2", "environmentId": "dev", "url": "https://example.com/dev2" }
          ]
        }
        """.trimIndent(),
        source = "/test",
    )

    private val springboardWithNoAllEnvsActivators = SpringboardFactory.fromJson(
        """
        {
          "name": "Env only",
          "environments": [{ "id": "dev", "name": "Dev" }],
          "apps": [{ "id": "app1", "name": "App" }],
          "resources": [{ "id": "res1", "name": "Resource" }],
          "activators": [
            { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com" }
          ]
        }
        """.trimIndent(),
        source = "/test",
    )

    @Test
    fun `allEnvsResources includes only resources with at least one ALL-envs activator, in declaration order`() {
        val resources = springboardWithAllEnvsAndEnvActivators.allEnvsResources()

        assertEquals(listOf("res1", "res3"), resources.map { it.id })
    }

    @Test
    fun `allEnvsResources is empty when springboard has no ALL-envs activators`() {
        assertTrue(springboardWithNoAllEnvsActivators.allEnvsResources().isEmpty())
    }

    @Test
    fun `envSpecificResources includes only resources with at least one env-specific activator, in declaration order`() {
        val resources = springboardWithAllEnvsAndEnvActivators.envSpecificResources()

        assertEquals(listOf("res2"), resources.map { it.id })
    }

    @Test
    fun `envSpecificResources is the full resource list when every resource has an env-specific activator`() {
        val springboard = SpringboardFactory.fromJson(
            """
            {
              "name": "Env-only",
              "environments": [{ "id": "dev", "name": "Dev" }],
              "apps": [{ "id": "app1", "name": "App" }],
              "resources": [
                { "id": "res1", "name": "One" },
                { "id": "res2", "name": "Two" }
              ],
              "activators": [
                { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/1" },
                { "type": "url", "appId": "app1", "resourceId": "res2", "environmentId": "dev", "url": "https://example.com/2" }
              ]
            }
            """.trimIndent(),
            source = "/test",
        )

        assertEquals(listOf("res1", "res2"), springboard.envSpecificResources().map { it.id })
    }

    @Test
    fun `hasAnyAllEnvsActivators is true when at least one ALL-envs activator exists`() {
        assertTrue(springboardWithAllEnvsAndEnvActivators.hasAnyAllEnvsActivators())
    }

    @Test
    fun `hasAnyAllEnvsActivators is false when springboard has no ALL-envs activators`() {
        assertFalse(springboardWithNoAllEnvsActivators.hasAnyAllEnvsActivators())
    }

    @Test
    fun `displayNameForEnvironmentId returns env name for configured env`() {
        assertEquals("Production", springboardWithAllEnvsAndEnvActivators.displayNameForEnvironmentId("prod"))
    }

    @Test
    fun `displayNameForEnvironmentId returns All Environments for ALL env id`() {
        assertEquals("All Environments", springboardWithAllEnvsAndEnvActivators.displayNameForEnvironmentId("ALL"))
    }

    @Test
    fun `displayNameForEnvironmentId falls back to raw id when nothing matches`() {
        assertEquals("unknown", springboardWithAllEnvsAndEnvActivators.displayNameForEnvironmentId("unknown"))
    }
}
