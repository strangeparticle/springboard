package com.strangeparticle.springboard.app.command

import com.strangeparticle.springboard.command.SpringboardCommand
import com.strangeparticle.springboard.command.SpringboardCommandResult
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class CommandApiMcpEndpointTest {
    @Test
    fun `mcp requires bearer token`() {
        withRunningServer { baseUrl, _ ->
            val (status, _) = postMcp("$baseUrl/mcp", initializeBody(), token = null)
            assertEquals(401, status)
        }
    }

    @Test
    fun `mcp initialize and tools list over http`() {
        withRunningServer { baseUrl, token ->
            val (initStatus, initBody) = postMcp("$baseUrl/mcp", initializeBody(), token)
            assertEquals(200, initStatus)
            assertTrue(initBody.contains("\"protocolVersion\""), initBody)

            val (listStatus, listBody) = postMcp("$baseUrl/mcp", toolsListBody(), token)
            assertEquals(200, listStatus)
            assertTrue(listBody.contains("\"get_snapshot\""), listBody)
        }
    }

    @Test
    fun `mcp get is method not allowed`() {
        withRunningServer { baseUrl, token ->
            val connection = URI("$baseUrl/mcp").toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            assertEquals(405, connection.responseCode)
        }
    }

    private fun withRunningServer(block: (baseUrl: String, token: String) -> Unit) {
        val token = "secret-token"
        val server = CommandApiServerDefaultImpl(
            executor = FakeMcpCommandExecutor(),
            discoveryFile = CommandApiDiscoveryFile(Files.createTempDirectory("springboard-mcp-endpoint-test")),
            preferredPort = 0,
            token = token,
        )
        val handle = server.start()
        try {
            block(handle.baseUrl, token)
        } finally {
            handle.stop()
        }
    }

    private fun initializeBody(): String =
        """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"0"}}}"""

    private fun toolsListBody(): String =
        """{"jsonrpc":"2.0","id":2,"method":"tools/list"}"""

    private fun postMcp(url: String, body: String, token: String?): Pair<Int, String> {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json, text/event-stream")
        if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
        connection.outputStream.use { it.write(body.toByteArray()) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        return status to text
    }

    private class FakeMcpCommandExecutor : SpringboardCommandExecutor {
        override suspend fun execute(command: SpringboardCommand): SpringboardCommandResult =
            SpringboardCommandResult.Success("ok")
    }
}
