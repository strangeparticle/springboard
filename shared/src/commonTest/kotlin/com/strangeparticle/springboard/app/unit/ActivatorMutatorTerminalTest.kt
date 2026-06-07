package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.domain.factory.SpringboardFactory
import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.domain.model.TerminalActivator
import com.strangeparticle.springboard.app.domain.mutator.SpringboardMutationError
import com.strangeparticle.springboard.app.domain.mutator.updateActivator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ActivatorMutatorTerminalTest {

    private val springboardWithTerminalActivator = SpringboardFactory.fromJson(
        """
        {
          "name": "Test",
          "environments": [{ "id": "staging", "name": "Staging" }],
          "apps": [{ "id": "app1", "name": "App One" }],
          "resources": [{ "id": "res1", "name": "Dashboard" }],
          "activators": [
            { "type": "term", "appId": "app1", "resourceId": "res1", "environmentId": "staging", "workingDirectory": "/my/dir" }
          ]
        }
        """.trimIndent(),
        "/test",
    )

    @Test
    fun `updating a terminal activator with a url-or-command payload is rejected`() {
        val activator = springboardWithTerminalActivator.activators
            .filterIsInstance<TerminalActivator>().single()
        val coordinate = Coordinate(activator.environmentId, activator.appId, activator.resourceId)

        val error = assertFailsWith<SpringboardMutationError> {
            updateActivator(springboardWithTerminalActivator, coordinate, newCommandTemplate = "ls")
        }
        assertEquals("wrong_field_for_type", error.code)
    }

    @Test
    fun `updating a terminal activator changes its working directory and command`() {
        val activator = springboardWithTerminalActivator.activators
            .filterIsInstance<TerminalActivator>().single()
        val coordinate = Coordinate(activator.environmentId, activator.appId, activator.resourceId)

        val updated = updateActivator(
            springboardWithTerminalActivator,
            coordinate,
            newWorkingDirectory = "/new/dir",
            newCommand = "npm test",
        )

        val terminal = updated.activators.filterIsInstance<TerminalActivator>().single()
        assertEquals("/new/dir", terminal.workingDirectory)
        assertEquals("npm test", terminal.command)
    }

    @Test
    fun `updating only the working directory leaves the command untouched`() {
        val springboard = SpringboardFactory.fromJson(
            """
            {
              "name": "Test",
              "environments": [{ "id": "staging", "name": "Staging" }],
              "apps": [{ "id": "app1", "name": "App One" }],
              "resources": [{ "id": "res1", "name": "Dashboard" }],
              "activators": [
                { "type": "term", "appId": "app1", "resourceId": "res1", "environmentId": "staging", "workingDirectory": "/old", "command": "ls" }
              ]
            }
            """.trimIndent(),
            "/test",
        )
        val activator = springboard.activators.filterIsInstance<TerminalActivator>().single()
        val coordinate = Coordinate(activator.environmentId, activator.appId, activator.resourceId)

        val updated = updateActivator(springboard, coordinate, newWorkingDirectory = "/new")

        val terminal = updated.activators.filterIsInstance<TerminalActivator>().single()
        assertEquals("/new", terminal.workingDirectory)
        assertEquals("ls", terminal.command)
    }
}
