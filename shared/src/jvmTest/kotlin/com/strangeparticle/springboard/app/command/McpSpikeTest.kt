package com.strangeparticle.springboard.app.command

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StreamableHttpServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.McpJson
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.success
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class McpSpikeTest {
    private fun freePort(): Int =
        ServerSocket(0, 50, InetAddress.getByName("127.0.0.1")).use { it.localPort }

    @Test
    fun `stateless mcp endpoint answers tools list as json`() {
        val port = freePort()
        val token = "spike-token"
        val engine = embeddedServer(CIO, host = "127.0.0.1", port = port) {
            // A3: single Authentication plugin with one bearer provider.
            install(Authentication) {
                bearer("command-api") {
                    authenticate { credential ->
                        if (credential.token == token) UserIdPrincipal("api") else null
                    }
                }
            }
            // Plugins the high-level helper would otherwise auto-install; we install them ourselves
            // so we can self-mount the transport (A2) inside the authenticated route.
            install(SSE)
            install(ContentNegotiation) { json(McpJson) }
            routing {
                authenticate("command-api") {
                    post("/mcp") {
                        val transport = StreamableHttpServerTransport(
                            StreamableHttpServerTransport.Configuration(
                                enableJsonResponse = true,
                                enableDnsRebindingProtection = true,
                                allowedHosts = listOf("localhost", "127.0.0.1", "[::1]"),
                            ),
                        ).also { it.setSessionIdGenerator(null) }
                        val server = Server(
                            serverInfo = Implementation(name = "SpikeServer", version = "0.0.1"),
                            options = ServerOptions(
                                capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = false)),
                            ),
                        ).apply {
                            addTool(name = "echo", description = "Echo back") { _ -> CallToolResult.success("ok") }
                        }
                        server.createSession(transport)
                        transport.handleRequest(session = null, call = call)
                    }
                }
            }
        }
        engine.start(wait = false)
        try {
            // 1) Unauthenticated request is rejected.
            val unauth = postMcp(port, body = initializeBody(), token = null)
            assertEquals(401, unauth.first)

            // 2) initialize succeeds and returns JSON (not an SSE stream).
            val (initStatus, initContentType, initBody) = postMcpFull(port, initializeBody(), token)
            assertEquals(200, initStatus)
            assertTrue(initContentType.contains("application/json"), "got: $initContentType")
            assertTrue(initBody.contains("\"protocolVersion\""), initBody)

            // 3) tools/list returns the echo tool.
            val (listStatus, _, listBody) = postMcpFull(port, toolsListBody(), token)
            assertEquals(200, listStatus)
            assertTrue(listBody.contains("\"echo\""), listBody)
        } finally {
            engine.stop()
        }
    }

    private fun initializeBody(): String =
        """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"spike","version":"0"}}}"""

    private fun toolsListBody(): String =
        """{"jsonrpc":"2.0","id":2,"method":"tools/list"}"""

    private fun postMcp(port: Int, body: String, token: String?): Pair<Int, String> {
        val (status, _, text) = postMcpFull(port, body, token)
        return status to text
    }

    private fun postMcpFull(port: Int, body: String, token: String?): Triple<Int, String, String> {
        val connection = URI("http://127.0.0.1:$port/mcp").toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json, text/event-stream")
        if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
        connection.outputStream.use { it.write(body.toByteArray()) }
        val status = connection.responseCode
        val contentType = connection.contentType ?: ""
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        return Triple(status, contentType, text)
    }
}
