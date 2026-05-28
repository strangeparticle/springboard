package com.strangeparticle.springboard.app.command

import com.strangeparticle.springboard.command.SpringboardCommand
import com.strangeparticle.springboard.command.SpringboardCommandErrorCode
import com.strangeparticle.springboard.command.SpringboardCommandJson
import com.strangeparticle.springboard.command.SpringboardCommandResult
import com.strangeparticle.springboard.command.dto.SpringboardCommandDto
import com.strangeparticle.springboard.command.dto.SpringboardCommandRequestEnvelopeDto
import com.strangeparticle.springboard.command.dto.SpringboardCommandResponseEnvelopeDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class CommandApiServerTest {
    @Test
    fun `token generator returns different high entropy tokens`() {
        val first = CommandApiTokenGenerator.generate()
        val second = CommandApiTokenGenerator.generate()

        assertTrue(first.length >= 32)
        assertNotEquals(first, second)
    }

    @Test
    fun `discovery file writes base url and token`() {
        val directory = Files.createTempDirectory("springboard-command-api-test")
        val discoveryFile = CommandApiDiscoveryFile(directory)
        val dto = CommandApiDiscoveryDto(
            baseUrl = "http://127.0.0.1:47382",
            token = "token-1",
            pid = 123,
            startedAt = "2026-05-27T12:34:56Z",
        )

        discoveryFile.write(dto)

        val text = directory.resolve("control-api.json").readText()
        assertTrue(text.contains("\"baseUrl\":\"http://127.0.0.1:47382\""))
        assertTrue(text.contains("\"token\":\"token-1\""))
    }

    @Test
    fun `server rejects command requests without bearer token`() {
        val server = CommandApiServerDefaultImpl(
            executor = FakeCommandExecutor(),
            discoveryFile = CommandApiDiscoveryFile(Files.createTempDirectory("springboard-command-api-test")),
            preferredPort = 0,
            token = "secret-token",
        )
        val handle = server.start()
        try {
            val request = SpringboardCommandRequestEnvelopeDto(
                requestId = "request-1",
                command = SpringboardCommandDto.Status,
            )

            val response = postCommand(handle.baseUrl, token = null, request)

            assertEquals(401, response.statusCode)
            assertEquals("unauthorized", response.body.resultCode())
        } finally {
            handle.stop()
        }
    }

    @Test
    fun `server accepts command requests with bearer token`() {
        val server = CommandApiServerDefaultImpl(
            executor = FakeCommandExecutor(),
            snapshotProvider = { """{"tabs":[],"activeTabId":null}""" },
            discoveryFile = CommandApiDiscoveryFile(Files.createTempDirectory("springboard-command-api-test")),
            preferredPort = 0,
            token = "secret-token",
        )
        val handle = server.start()
        try {
            val request = SpringboardCommandRequestEnvelopeDto(
                requestId = "request-1",
                command = SpringboardCommandDto.Status,
            )

            val response = postCommand(handle.baseUrl, token = "secret-token", request)

            assertEquals(200, response.statusCode)
            assertEquals("success", response.body.resultType())
        } finally {
            handle.stop()
        }
    }

    @Test
    fun `help endpoint describes command-specific endpoint and bearer auth`() {
        val server = CommandApiServerDefaultImpl(
            executor = FakeCommandExecutor(),
            snapshotProvider = { """{"tabs":[],"activeTabId":null}""" },
            discoveryFile = CommandApiDiscoveryFile(Files.createTempDirectory("springboard-command-api-test")),
            preferredPort = 0,
            token = "secret-token",
        )
        val handle = server.start()
        try {
            val response = getText(handle.baseUrl, "/api/help", token = null)

            assertEquals(200, response.statusCode)
            assertTrue(response.body.contains("\"name\":\"Springboard Command API\""))
            assertTrue(response.body.contains("\"/api/status\""))
            assertTrue(response.body.contains("\"/api/commands/activate-coordinate\""))
            assertTrue(response.body.contains("\"/api/commands/open-springboard\""))
            assertTrue(response.body.contains("\"/api/commands/switch-tab\""))
            assertTrue(response.body.contains("\"/api/commands/show-guidance\""))
            assertTrue(response.body.contains("\"/api/snapshot\""))
            assertTrue(response.body.contains("\"type\":\"bearer\""))
            assertTrue(response.body.contains("Use the reserved id ALL"))
        } finally {
            handle.stop()
        }
    }

    @Test
    fun `openapi json is deferred to a future version`() {
        val server = CommandApiServerDefaultImpl(
            executor = FakeCommandExecutor(),
            snapshotProvider = { """{"tabs":[],"activeTabId":null}""" },
            discoveryFile = CommandApiDiscoveryFile(Files.createTempDirectory("springboard-command-api-test")),
            preferredPort = 0,
            token = "secret-token",
        )
        val handle = server.start()
        try {
            val response = getText(handle.baseUrl, "/openapi.json", token = null)

            assertEquals(404, response.statusCode)
        } finally {
            handle.stop()
        }
    }

    @Test
    fun `command catalog endpoint returns command metadata`() {
        val server = CommandApiServerDefaultImpl(
            executor = FakeCommandExecutor(),
            snapshotProvider = { """{"tabs":[],"activeTabId":null}""" },
            discoveryFile = CommandApiDiscoveryFile(Files.createTempDirectory("springboard-command-api-test")),
            preferredPort = 0,
            token = "secret-token",
        )
        val handle = server.start()
        try {
            val response = getText(handle.baseUrl, "/api/commands", token = "secret-token")
            val root = Json.parseToJsonElement(response.body).jsonObject
            val commands = root.getValue("commands").jsonArray

            assertEquals(200, response.statusCode)
            assertTrue(commands.any {
                it.jsonObject.getValue("id").jsonPrimitive.content == "activateCoordinate"
            })
        } finally {
            handle.stop()
        }
    }

    @Test
    fun `snapshot endpoint returns current snapshot`() {
        val server = CommandApiServerDefaultImpl(
            executor = FakeCommandExecutor(),
            snapshotProvider = { """{"tabs":[{"tabId":"tab-1","label":"Test"}],"activeTabId":"tab-1"}""" },
            discoveryFile = CommandApiDiscoveryFile(Files.createTempDirectory("springboard-command-api-test")),
            preferredPort = 0,
            token = "secret-token",
        )
        val handle = server.start()
        try {
            val response = getText(handle.baseUrl, "/api/snapshot", token = "secret-token")

            assertEquals(200, response.statusCode)
            assertTrue(response.body.contains("\"activeTabId\":\"tab-1\""))
        } finally {
            handle.stop()
        }
    }

    @Test
    fun `activate coordinate endpoint maps request body to command`() {
        val executor = RecordingCommandExecutor()
        val server = CommandApiServerDefaultImpl(
            executor = executor,
            snapshotProvider = { """{"tabs":[],"activeTabId":null}""" },
            discoveryFile = CommandApiDiscoveryFile(Files.createTempDirectory("springboard-command-api-test")),
            preferredPort = 0,
            token = "secret-token",
        )
        val handle = server.start()
        try {
            val response = postJson(
                baseUrl = handle.baseUrl,
                path = "/api/commands/activate-coordinate",
                token = "secret-token",
                body = """
                {
                  "requestId": "request-activate",
                  "tabId": "tab-1",
                  "environmentId": "ALL",
                  "appId": "github",
                  "resourceId": "repo"
                }
                """.trimIndent(),
            )

            assertEquals(200, response.statusCode)
            assertEquals(
                SpringboardCommand.ActivateCoordinate(
                    tabId = "tab-1",
                    environmentId = "ALL",
                    appId = "github",
                    resourceId = "repo",
                ),
                executor.commands.single(),
            )
        } finally {
            handle.stop()
        }
    }

    @Test
    fun `status endpoint executes status command`() {
        val executor = RecordingCommandExecutor()
        val server = CommandApiServerDefaultImpl(
            executor = executor,
            snapshotProvider = { """{"tabs":[],"activeTabId":null}""" },
            discoveryFile = CommandApiDiscoveryFile(Files.createTempDirectory("springboard-command-api-test")),
            preferredPort = 0,
            token = "secret-token",
        )
        val handle = server.start()
        try {
            val response = getText(handle.baseUrl, "/api/status", token = "secret-token")

            assertEquals(200, response.statusCode)
            assertTrue(response.body.contains("\"protocolVersion\":1"))
            assertTrue(response.body.contains("\"requestId\":null"))
            assertEquals(listOf<SpringboardCommand>(SpringboardCommand.Status), executor.commands)
        } finally {
            handle.stop()
        }
    }

    @Test
    fun `status endpoint rejects missing bearer token with failure envelope`() {
        val server = CommandApiServerDefaultImpl(
            executor = RecordingCommandExecutor(),
            snapshotProvider = { """{"tabs":[],"activeTabId":null}""" },
            discoveryFile = CommandApiDiscoveryFile(Files.createTempDirectory("springboard-command-api-test")),
            preferredPort = 0,
            token = "secret-token",
        )
        val handle = server.start()
        try {
            val response = getText(handle.baseUrl, "/api/status", token = null)

            assertEquals(401, response.statusCode)
            assertTrue(response.body.contains("\"protocolVersion\":1"))
            assertTrue(response.body.contains("\"requestId\":null"))
            assertTrue(response.body.contains("\"code\":\"unauthorized\""))
        } finally {
            handle.stop()
        }
    }

    @Test
    fun `status endpoint rejects wrong bearer token with failure envelope`() {
        val server = CommandApiServerDefaultImpl(
            executor = RecordingCommandExecutor(),
            snapshotProvider = { """{"tabs":[],"activeTabId":null}""" },
            discoveryFile = CommandApiDiscoveryFile(Files.createTempDirectory("springboard-command-api-test")),
            preferredPort = 0,
            token = "secret-token",
        )
        val handle = server.start()
        try {
            val response = getText(handle.baseUrl, "/api/status", token = "wrong-token")

            assertEquals(401, response.statusCode)
            assertTrue(response.body.contains("\"protocolVersion\":1"))
            assertTrue(response.body.contains("\"requestId\":null"))
            assertTrue(response.body.contains("\"code\":\"unauthorized\""))
        } finally {
            handle.stop()
        }
    }

    @Test
    fun `open springboard endpoint maps request body to command`() {
        val executor = RecordingCommandExecutor()
        val server = CommandApiServerDefaultImpl(
            executor = executor,
            snapshotProvider = { """{"tabs":[],"activeTabId":null}""" },
            discoveryFile = CommandApiDiscoveryFile(Files.createTempDirectory("springboard-command-api-test")),
            preferredPort = 0,
            token = "secret-token",
        )
        val handle = server.start()
        try {
            val response = postJson(
                baseUrl = handle.baseUrl,
                path = "/api/commands/open-springboard",
                token = "secret-token",
                body = """{"requestId":"request-open","source":"/tmp/dev.springboard.json","inNewTab":true}""",
            )

            assertEquals(200, response.statusCode)
            assertEquals(
                SpringboardCommand.OpenSpringboard(
                    source = "/tmp/dev.springboard.json",
                    inNewTab = true,
                ),
                executor.commands.single(),
            )
        } finally {
            handle.stop()
        }
    }

    @Test
    fun `switch tab endpoint maps request body to command`() {
        val executor = RecordingCommandExecutor()
        val server = CommandApiServerDefaultImpl(
            executor = executor,
            snapshotProvider = { """{"tabs":[],"activeTabId":null}""" },
            discoveryFile = CommandApiDiscoveryFile(Files.createTempDirectory("springboard-command-api-test")),
            preferredPort = 0,
            token = "secret-token",
        )
        val handle = server.start()
        try {
            val response = postJson(
                baseUrl = handle.baseUrl,
                path = "/api/commands/switch-tab",
                token = "secret-token",
                body = """{"requestId":"request-switch","tabIndex":2}""",
            )

            assertEquals(200, response.statusCode)
            assertEquals(SpringboardCommand.SwitchTab(tabIndex = 2), executor.commands.single())
        } finally {
            handle.stop()
        }
    }

    @Test
    fun `show guidance endpoint maps request body to command`() {
        val executor = RecordingCommandExecutor()
        val server = CommandApiServerDefaultImpl(
            executor = executor,
            snapshotProvider = { """{"tabs":[],"activeTabId":null}""" },
            discoveryFile = CommandApiDiscoveryFile(Files.createTempDirectory("springboard-command-api-test")),
            preferredPort = 0,
            token = "secret-token",
        )
        val handle = server.start()
        try {
            val response = postJson(
                baseUrl = handle.baseUrl,
                path = "/api/commands/show-guidance",
                token = "secret-token",
                body = """
                {
                  "requestId": "request-guidance",
                  "environmentId": "ALL",
                  "appId": "github",
                  "resourceId": "repo"
                }
                """.trimIndent(),
            )

            assertEquals(200, response.statusCode)
            assertEquals(
                SpringboardCommand.ShowGuidance(
                    environmentId = "ALL",
                    appId = "github",
                    resourceId = "repo",
                ),
                executor.commands.single(),
            )
        } finally {
            handle.stop()
        }
    }

    @Test
    fun `server falls back when preferred port is occupied`() {
        ServerSocket(0, 50, InetAddress.getByName("127.0.0.1")).use { blocker ->
            val server = CommandApiServerDefaultImpl(
                executor = FakeCommandExecutor(),
                discoveryFile = CommandApiDiscoveryFile(Files.createTempDirectory("springboard-command-api-test")),
                preferredPort = blocker.localPort,
                token = "secret-token",
            )
            val handle = server.start()
            try {
                val chosenPort = URI(handle.baseUrl).port
                assertNotEquals(blocker.localPort, chosenPort)
            } finally {
                handle.stop()
            }
        }
    }

    private fun postCommand(
        baseUrl: String,
        token: String?,
        request: SpringboardCommandRequestEnvelopeDto,
    ): HttpResponse {
        val connection = URI("$baseUrl/api/commands").toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        if (token != null) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
        connection.outputStream.use { output ->
            output.write(SpringboardCommandJson.encodeRequest(request).encodeToByteArray())
        }

        val statusCode = connection.responseCode
        val stream = if (statusCode < 400) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader().use { it.readText() }
        return HttpResponse(
            statusCode = statusCode,
            body = SpringboardCommandJson.decodeResponse(body),
        )
    }

    private fun postJson(
        baseUrl: String,
        path: String,
        token: String?,
        body: String,
    ): TextResponse {
        val connection = URI("$baseUrl$path").toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        if (token != null) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
        connection.outputStream.use { output -> output.write(body.encodeToByteArray()) }
        return connection.readTextResponse()
    }

    private fun getText(baseUrl: String, path: String, token: String?): TextResponse {
        val connection = URI("$baseUrl$path").toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        if (token != null) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
        return connection.readTextResponse()
    }

    private fun HttpURLConnection.readTextResponse(): TextResponse {
        val statusCode = responseCode
        val stream = if (statusCode < 400) inputStream else errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        return TextResponse(statusCode, body)
    }

    private fun SpringboardCommandResponseEnvelopeDto.resultType(): String = when (result) {
        is com.strangeparticle.springboard.command.dto.SpringboardCommandResultDto.Success -> "success"
        is com.strangeparticle.springboard.command.dto.SpringboardCommandResultDto.Failure -> "failure"
    }

    private fun SpringboardCommandResponseEnvelopeDto.resultCode(): String =
        (result as com.strangeparticle.springboard.command.dto.SpringboardCommandResultDto.Failure).code

    private data class HttpResponse(
        val statusCode: Int,
        val body: SpringboardCommandResponseEnvelopeDto,
    )

    private data class TextResponse(
        val statusCode: Int,
        val body: String,
    )

    private class FakeCommandExecutor : SpringboardCommandExecutor {
        override suspend fun execute(command: SpringboardCommand): SpringboardCommandResult {
            return when (command) {
                SpringboardCommand.Status -> SpringboardCommandResult.Success("Springboard is running.")
                is SpringboardCommand.ActivateCoordinate -> SpringboardCommandResult.Failure(
                    code = SpringboardCommandErrorCode.CoordinateNotFound,
                    message = "Not found.",
                )
                is SpringboardCommand.OpenSpringboard -> SpringboardCommandResult.Success("Opened.")
                is SpringboardCommand.SwitchTab -> SpringboardCommandResult.Success("Switched.")
                is SpringboardCommand.ShowGuidance -> SpringboardCommandResult.Success("Showing guidance.")
            }
        }
    }

    private class RecordingCommandExecutor : SpringboardCommandExecutor {
        val commands = mutableListOf<SpringboardCommand>()

        override suspend fun execute(command: SpringboardCommand): SpringboardCommandResult {
            commands += command
            return SpringboardCommandResult.Success("Recorded.")
        }
    }
}
