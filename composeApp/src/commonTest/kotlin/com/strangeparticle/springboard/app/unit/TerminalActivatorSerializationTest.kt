package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.domain.factory.SpringboardFactory
import com.strangeparticle.springboard.app.domain.factory.dto.TerminalActivatorDto
import com.strangeparticle.springboard.app.domain.factory.springboardToDto
import com.strangeparticle.springboard.app.domain.model.TerminalActivator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TerminalActivatorSerializationTest {

    private fun jsonWithTermActivator(activatorJson: String) = """
    {
      "name": "Test",
      "environments": [{ "id": "staging", "name": "Staging" }],
      "apps": [{ "id": "app1", "name": "App One" }],
      "resources": [{ "id": "res1", "name": "Dashboard" }],
      "activators": [ $activatorJson ]
    }
    """.trimIndent()

    @Test
    fun `term activator with command parses into a TerminalActivator`() {
        val json = jsonWithTermActivator(
            """{ "type": "term", "appId": "app1", "resourceId": "res1", "environmentId": "staging", "workingDirectory": "/my/dir", "command": "ls -la" }""",
        )

        val springboard = SpringboardFactory.fromJson(json, "/test")
        val terminalActivators = springboard.activators.filterIsInstance<TerminalActivator>()

        assertEquals(1, terminalActivators.size)
        val activator = terminalActivators.first()
        assertEquals("/my/dir", activator.workingDirectory)
        assertEquals("ls -la", activator.command)
    }

    @Test
    fun `term activator without command parses with null command`() {
        val json = jsonWithTermActivator(
            """{ "type": "term", "appId": "app1", "resourceId": "res1", "environmentId": "staging", "workingDirectory": "/my/dir" }""",
        )

        val springboard = SpringboardFactory.fromJson(json, "/test")
        val activator = springboard.activators.filterIsInstance<TerminalActivator>().first()

        assertEquals("/my/dir", activator.workingDirectory)
        assertNull(activator.command)
    }

    @Test
    fun `term activator round-trips back to a TerminalActivatorDto with fields preserved`() {
        val json = jsonWithTermActivator(
            """{ "type": "term", "appId": "app1", "resourceId": "res1", "environmentId": "staging", "workingDirectory": "/my/dir", "command": "git status" }""",
        )

        val springboard = SpringboardFactory.fromJson(json, "/test")
        val dto = springboardToDto(springboard)
        val terminalDto = dto.activators.filterIsInstance<TerminalActivatorDto>().single()

        assertEquals("app1", terminalDto.appId)
        assertEquals("res1", terminalDto.resourceId)
        assertEquals("staging", terminalDto.environmentId)
        assertEquals("/my/dir", terminalDto.workingDirectory)
        assertEquals("git status", terminalDto.command)
    }
}
