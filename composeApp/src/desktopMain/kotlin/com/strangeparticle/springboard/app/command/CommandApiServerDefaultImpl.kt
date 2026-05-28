package com.strangeparticle.springboard.app.command

import com.strangeparticle.luther.toolcall.ToolCallDispatcher
import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallExecutionResult
import com.strangeparticle.luther.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.toolcall.ToolCallRegistry
import com.strangeparticle.springboard.app.luther.SpringboardToolCallHandlerResponse
import com.strangeparticle.springboard.app.luther.toolcall.createSpringboardToolCallRegistry
import com.strangeparticle.springboard.command.SpringboardCommandErrorCode
import com.strangeparticle.springboard.command.SpringboardCommandJson
import com.strangeparticle.springboard.command.SpringboardCommandResult
import com.strangeparticle.springboard.command.SpringboardCommandCatalog
import com.strangeparticle.springboard.command.dto.SpringboardActivateCoordinateRequestDto
import com.strangeparticle.springboard.command.dto.SpringboardCommandDto
import com.strangeparticle.springboard.command.dto.SpringboardCommandDtoMapper
import com.strangeparticle.springboard.command.dto.SpringboardCommandRequestEnvelopeDto
import com.strangeparticle.springboard.command.dto.SpringboardCommandResponseEnvelopeDto
import com.strangeparticle.springboard.command.dto.SpringboardCommandResultDto
import com.strangeparticle.springboard.command.dto.SpringboardOpenSpringboardRequestDto
import com.strangeparticle.springboard.command.dto.SpringboardShowGuidanceRequestDto
import com.strangeparticle.springboard.command.dto.SpringboardSwitchTabRequestDto
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.header
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.net.InetAddress
import java.net.ServerSocket
import java.time.Instant

internal class CommandApiServerDefaultImpl(
    private val executor: SpringboardCommandExecutor,
    private val snapshotProvider: () -> String = { """{"tabs":[],"activeTabId":null}""" },
    private val discoveryFile: CommandApiDiscoveryFile = CommandApiDiscoveryFile(),
    private val preferredPort: Int = 47382,
    private val token: String = CommandApiTokenGenerator.generate(),
    private val toolCallRegistry: ToolCallRegistry = createSpringboardToolCallRegistry(),
    private val toolCallExecutionContext: ToolCallExecutionContext? = null,
) : CommandApiServer {
    private val toolCallDispatcher = ToolCallDispatcher(toolCallRegistry)

    override fun start(): CommandApiServerHandle {
        val port = choosePort()
        val baseUrl = "http://127.0.0.1:$port"
        val engine = embeddedServer(CIO, host = "127.0.0.1", port = port) {
            routing {
                get("/api/help") {
                    call.respondText(
                        text = helpJson(baseUrl),
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.OK,
                    )
                }
                authenticatedGet("/api/commands") {
                    respondText(
                        text = commandCatalogJson(),
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.OK,
                    )
                }
                authenticatedGet("/api/tools") {
                    respondText(
                        text = toolCatalogJson(),
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.OK,
                    )
                }
                authenticatedGet("/api/snapshot") {
                    respondText(
                        text = snapshotProvider(),
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.OK,
                    )
                }
                authenticatedGet("/api/status") {
                    respondExecutedCommand(
                        requestId = null,
                        commandDto = SpringboardCommandDto.Status,
                    )
                }
                authenticatedPost("/api/commands") {
                    val request = runCatching {
                        SpringboardCommandJson.decodeRequest(receiveText())
                    }.getOrElse {
                        respondCommand(
                            status = HttpStatusCode.BadRequest,
                            response = failureResponse(
                                requestId = null,
                                code = SpringboardCommandErrorCode.InvalidRequest,
                                message = "Invalid command request JSON.",
                            ),
                        )
                        return@authenticatedPost
                    }

                    if (request.protocolVersion != 1) {
                        respondCommand(
                            status = HttpStatusCode.BadRequest,
                            response = failureResponse(
                                requestId = request.requestId,
                                code = SpringboardCommandErrorCode.ProtocolVersionUnsupported,
                                message = "Unsupported command protocol version: ${request.protocolVersion}.",
                            ),
                        )
                        return@authenticatedPost
                    }

                    respondExecutedCommand(
                        requestId = request.requestId,
                        commandDto = request.command,
                    )
                }
                authenticatedPost("/api/commands/activate-coordinate") {
                    val request = runCatching {
                        SpringboardCommandJson.json.decodeFromString<SpringboardActivateCoordinateRequestDto>(receiveText())
                    }.getOrElse {
                        respondCommand(
                            status = HttpStatusCode.BadRequest,
                            response = failureResponse(
                                requestId = null,
                                code = SpringboardCommandErrorCode.InvalidRequest,
                                message = "Invalid activate-coordinate request JSON.",
                            ),
                        )
                        return@authenticatedPost
                    }

                    respondExecutedCommand(
                        requestId = request.requestId,
                        commandDto = SpringboardCommandDtoMapper.toCommandDto(request),
                    )
                }
                authenticatedPost("/api/commands/open-springboard") {
                    val request = runCatching {
                        SpringboardCommandJson.json.decodeFromString<SpringboardOpenSpringboardRequestDto>(receiveText())
                    }.getOrElse {
                        respondCommand(
                            status = HttpStatusCode.BadRequest,
                            response = failureResponse(
                                requestId = null,
                                code = SpringboardCommandErrorCode.InvalidRequest,
                                message = "Invalid open-springboard request JSON.",
                            ),
                        )
                        return@authenticatedPost
                    }

                    respondExecutedCommand(
                        requestId = request.requestId,
                        commandDto = SpringboardCommandDtoMapper.toCommandDto(request),
                    )
                }
                authenticatedPost("/api/commands/switch-tab") {
                    val request = runCatching {
                        SpringboardCommandJson.json.decodeFromString<SpringboardSwitchTabRequestDto>(receiveText())
                    }.getOrElse {
                        respondCommand(
                            status = HttpStatusCode.BadRequest,
                            response = failureResponse(
                                requestId = null,
                                code = SpringboardCommandErrorCode.InvalidRequest,
                                message = "Invalid switch-tab request JSON.",
                            ),
                        )
                        return@authenticatedPost
                    }

                    respondExecutedCommand(
                        requestId = request.requestId,
                        commandDto = SpringboardCommandDtoMapper.toCommandDto(request),
                    )
                }
                authenticatedPost("/api/commands/show-guidance") {
                    val request = runCatching {
                        SpringboardCommandJson.json.decodeFromString<SpringboardShowGuidanceRequestDto>(receiveText())
                    }.getOrElse {
                        respondCommand(
                            status = HttpStatusCode.BadRequest,
                            response = failureResponse(
                                requestId = null,
                                code = SpringboardCommandErrorCode.InvalidRequest,
                                message = "Invalid show-guidance request JSON.",
                            ),
                        )
                        return@authenticatedPost
                    }

                    respondExecutedCommand(
                        requestId = request.requestId,
                        commandDto = SpringboardCommandDtoMapper.toCommandDto(request),
                    )
                }
                authenticatedPost("/api/tools/{toolName}") {
                    val toolName = parameters["toolName"].orEmpty()
                    val body = receiveText()
                    val parsedRequest = parseToolRequest(body).getOrElse {
                        respondCommand(
                            status = HttpStatusCode.BadRequest,
                            response = SpringboardCommandResponseEnvelopeDto(
                                requestId = null,
                                result = SpringboardCommandResultDto.Failure(
                                    code = SpringboardCommandErrorCode.InvalidRequest.wireValue,
                                    message = "Invalid tool request JSON.",
                                ),
                            ),
                        )
                        return@authenticatedPost
                    }
                    respondExecutedTool(
                        requestId = parsedRequest.requestId,
                        toolName = toolName,
                        argumentsAsJsonString = parsedRequest.argumentsAsJsonString,
                    )
                }
            }
        }

        engine.start(wait = false)
        discoveryFile.write(
            CommandApiDiscoveryDto(
                baseUrl = baseUrl,
                token = token,
                pid = ProcessHandle.current().pid(),
                startedAt = Instant.now().toString(),
            )
        )
        val shutdownHook = Thread {
            discoveryFile.delete()
        }
        Runtime.getRuntime().addShutdownHook(shutdownHook)
        return DefaultCommandApiServerHandle(
            baseUrl = baseUrl,
            token = token,
            stopServer = { engine.stop() },
            discoveryFile = discoveryFile,
            shutdownHook = shutdownHook,
        )
    }

    private fun choosePort(): Int {
        if (preferredPort > 0 && isPortAvailable(preferredPort)) {
            return preferredPort
        }
        ServerSocket(0, 50, InetAddress.getByName("127.0.0.1")).use { socket ->
            return socket.localPort
        }
    }

    private fun isPortAvailable(port: Int): Boolean =
        runCatching {
            ServerSocket(port, 50, InetAddress.getByName("127.0.0.1")).use { true }
        }.getOrDefault(false)

    private fun failureResponse(
        requestId: String?,
        code: SpringboardCommandErrorCode,
        message: String,
    ): SpringboardCommandResponseEnvelopeDto =
        SpringboardCommandResponseEnvelopeDto(
            requestId = requestId,
            result = SpringboardCommandDtoMapper.toDto(
                SpringboardCommandResult.Failure(
                    code = code,
                    message = message,
                )
            ),
        )

    private fun unauthorizedResponse(): SpringboardCommandResponseEnvelopeDto =
        failureResponse(
            requestId = null,
            code = SpringboardCommandErrorCode.Unauthorized,
            message = "Missing or invalid command API token.",
        )

    private fun ApplicationRequest.hasValidToken(): Boolean =
        header("Authorization") == "Bearer $token"

    private fun Route.authenticatedGet(
        path: String,
        handler: suspend ApplicationCall.() -> Unit,
    ) {
        get(path) {
            if (call.respondUnauthorizedIfNeeded()) {
                call.handler()
            }
        }
    }

    private fun Route.authenticatedPost(
        path: String,
        handler: suspend ApplicationCall.() -> Unit,
    ) {
        post(path) {
            if (call.respondUnauthorizedIfNeeded()) {
                call.handler()
            }
        }
    }

    private suspend fun ApplicationCall.respondUnauthorizedIfNeeded(): Boolean {
        if (request.hasValidToken()) {
            return true
        }
        respondCommand(
            status = HttpStatusCode.Unauthorized,
            response = unauthorizedResponse(),
        )
        return false
    }

    private fun commandCatalogJson(): String {
        val catalog = buildJsonObject {
            put("protocolVersion", 1)
            putJsonArray("commands") {
                SpringboardCommandCatalog.definitions.forEach { definition ->
                    add(SpringboardCommandJson.json.encodeToJsonElement(SpringboardCommandDtoMapper.toDto(definition)))
                }
            }
        }
        return SpringboardCommandJson.json.encodeToString(catalog)
    }

    private fun toolCatalogJson(): String {
        val catalog = buildJsonObject {
            put("protocolVersion", 1)
            putJsonArray("tools") {
                toolCallRegistry.getHandlers().forEach { tool ->
                    add(buildJsonObject {
                        put("name", tool.providerToolId)
                        put("description", tool.description)
                        put("requiresUserConfirmation", tool.requiresUserConfirmation)
                        put("endpoint", "/api/tools/${tool.providerToolId}")
                        put("schema", tool.schema)
                    })
                }
            }
        }
        return SpringboardCommandJson.json.encodeToString(catalog)
    }

    private fun helpJson(baseUrl: String): String {
        val help = buildJsonObject {
            put("name", "Springboard Command API")
            put("protocolVersion", 1)
            put("baseUrl", baseUrl)
            putJsonObject("auth") {
                put("type", "bearer")
                put("description", "Read the token from the local discovery file and send Authorization: Bearer <token>.")
            }
            put("discoveryFile", discoveryFile.path.toString())
            putJsonArray("endpoints") {
                add(buildJsonObject {
                    put("method", "GET")
                    put("path", "/api/help")
                    put("description", "Returns this command API help document.")
                    put("requiresAuth", false)
                })
                add(buildJsonObject {
                    put("method", "GET")
                    put("path", "/api/commands")
                    put("description", "Returns machine-readable command metadata.")
                    put("requiresAuth", true)
                })
                add(buildJsonObject {
                    put("method", "GET")
                    put("path", "/api/tools")
                    put("description", "Returns the full provider-visible tool catalog used by Springboard's assistant.")
                    put("requiresAuth", true)
                })
                add(buildJsonObject {
                    put("method", "GET")
                    put("path", "/api/snapshot")
                    put("description", "Returns the current Springboard app snapshot.")
                    put("requiresAuth", true)
                })
                add(buildJsonObject {
                    put("method", "GET")
                    put("path", "/api/status")
                    put("description", "Executes the status command and returns app reachability information.")
                    put("requiresAuth", true)
                })
                add(buildJsonObject {
                    put("method", "POST")
                    put("path", "/api/commands")
                    put("description", "Executes a generic command request envelope.")
                    put("requiresAuth", true)
                })
                // every tool is exposed as a callable endpoint
                add(buildJsonObject {
                    put("method", "POST")
                    put("path", "/api/tools/{toolName}")
                    put("description", "Executes a provider-visible Springboard tool with JSON arguments.")
                    put("requiresAuth", true)
                    // This is the generic HTTP wrapper. Each tool's actual argument shape
                    // is published in GET /api/tools as that tool's provider schema.
                    putJsonObject("exampleRequest") {
                        put("requestId", "request-tool")
                        putJsonObject("arguments") {
                            put("tab_id", "tab-1")
                        }
                    }
                })
                add(buildJsonObject {
                    put("method", "POST")
                    put("path", "/api/commands/activate-coordinate")
                    put("description", "Activates matching Springboard activators for a coordinate.")
                    put("requiresAuth", true)
                    putJsonObject("exampleRequest") {
                        put("requestId", "request-activate")
                        put("environmentId", "ALL")
                        put("appId", "github")
                        put("resourceId", "repo")
                    }
                })
                add(buildJsonObject {
                    put("method", "POST")
                    put("path", "/api/commands/open-springboard")
                    put("description", "Opens a Springboard JSON source.")
                    put("requiresAuth", true)
                    putJsonObject("exampleRequest") {
                        put("requestId", "request-open")
                        put("source", "/Users/example/springboards/dev.springboard.json")
                        put("inNewTab", true)
                    }
                })
                add(buildJsonObject {
                    put("method", "POST")
                    put("path", "/api/commands/switch-tab")
                    put("description", "Switches to an open tab by one-based index.")
                    put("requiresAuth", true)
                    putJsonObject("exampleRequest") {
                        put("requestId", "request-switch")
                        put("tabIndex", 2)
                    }
                })
                add(buildJsonObject {
                    put("method", "POST")
                    put("path", "/api/commands/show-guidance")
                    put("description", "Selects a coordinate so its guidance is shown in the app.")
                    put("requiresAuth", true)
                    putJsonObject("exampleRequest") {
                        put("requestId", "request-guidance")
                        put("environmentId", "ALL")
                        put("appId", "github")
                        put("resourceId", "repo")
                    }
                })
            }
            putJsonArray("commands") {
                SpringboardCommandCatalog.definitions.forEach { definition ->
                    add(SpringboardCommandJson.json.encodeToJsonElement(SpringboardCommandDtoMapper.toDto(definition)))
                }
            }
            put("toolCatalog", "/api/tools")
            putJsonObject("response") {
                putJsonObject("successExample") {
                    put("protocolVersion", 1)
                    put("requestId", "request-activate")
                    putJsonObject("result") {
                        put("type", "success")
                        put("message", "Activated 1 activator.")
                    }
                }
                putJsonObject("failureExample") {
                    put("protocolVersion", 1)
                    put("requestId", "request-activate")
                    putJsonObject("result") {
                        put("type", "failure")
                        put("code", SpringboardCommandErrorCode.CoordinateNotFound.wireValue)
                        put("message", "No activators resolved.")
                    }
                }
            }
        }
        return SpringboardCommandJson.json.encodeToString(help)
    }

    private fun parseToolRequest(body: String): Result<CommandApiToolRequest> = runCatching {
        val root = SpringboardCommandJson.json.parseToJsonElement(body)
        val rootObject = root.jsonObject
        val requestId = rootObject["requestId"]?.jsonPrimitive?.contentOrNull
        // Callers may send either {"requestId": "...", "arguments": {...}} or the raw
        // provider-style argument object. Tool-specific DTO decoding happens in the
        // registered ToolCallHandler, not in the HTTP transport layer.
        val arguments = rootObject["arguments"] ?: root
        CommandApiToolRequest(
            requestId = requestId,
            argumentsAsJsonString = SpringboardCommandJson.json.encodeToString(arguments),
        )
    }

    private suspend fun ApplicationCall.respondExecutedCommand(
        requestId: String?,
        commandDto: SpringboardCommandDto,
    ) {
        val validationResult = SpringboardCommandDtoMapper.toCommandResult(commandDto)
        if (validationResult is SpringboardCommandResult.Failure) {
            respondCommand(
                status = HttpStatusCode.BadRequest,
                response = SpringboardCommandResponseEnvelopeDto(
                    requestId = requestId,
                    result = SpringboardCommandDtoMapper.toDto(validationResult),
                ),
            )
            return
        }

        val result = runCatching {
            executor.execute(SpringboardCommandDtoMapper.toCommand(commandDto))
        }.getOrElse {
            SpringboardCommandResult.Failure(
                code = SpringboardCommandErrorCode.InternalError,
                message = "Command execution failed.",
            )
        }
        respondCommand(
            status = HttpStatusCode.OK,
            response = SpringboardCommandResponseEnvelopeDto(
                requestId = requestId,
                result = SpringboardCommandDtoMapper.toDto(result),
            ),
        )
    }

    private suspend fun ApplicationCall.respondExecutedTool(
        requestId: String?,
        toolName: String,
        argumentsAsJsonString: String,
    ) {
        val context = toolCallExecutionContext
        if (context == null) {
            respondCommand(
                status = HttpStatusCode.ServiceUnavailable,
                response = SpringboardCommandResponseEnvelopeDto(
                    requestId = requestId,
                    result = SpringboardCommandResultDto.Failure(
                        code = "toolExecutionUnavailable",
                        message = "Tool execution is not available in this Springboard process.",
                    ),
                ),
            )
            return
        }

        val response = runCatching {
            toolCallDispatcher.execute(
                toolCallId = requestId ?: "command-api-$toolName",
                providerToolId = toolName,
                argumentsAsJsonString = argumentsAsJsonString,
                context = context,
            )
        }.getOrElse {
            ToolCallExecutionResult(
                success = false,
                message = "Tool execution failed.",
                code = "internal_error",
            )
        }

        respondCommand(
            status = HttpStatusCode.OK,
            response = SpringboardCommandResponseEnvelopeDto(
                requestId = requestId,
                result = response.toCommandResultDto(),
            ),
        )
    }

    private fun ToolCallHandlerResponse.toCommandResultDto(): SpringboardCommandResultDto {
        val providerContent = toProviderMessageContent(SpringboardCommandJson.json)
        val data = runCatching { SpringboardCommandJson.json.parseToJsonElement(providerContent) }.getOrNull()
        return when (this) {
            is SpringboardToolCallHandlerResponse -> if (success) {
                SpringboardCommandResultDto.Success(message = message, data = data)
            } else {
                SpringboardCommandResultDto.Failure(
                    code = code ?: "toolExecutionFailed",
                    message = message ?: "Tool execution failed.",
                    details = data,
                )
            }
            is ToolCallExecutionResult -> if (success) {
                SpringboardCommandResultDto.Success(message = message, data = data)
            } else {
                SpringboardCommandResultDto.Failure(
                    code = code ?: "toolExecutionFailed",
                    message = message ?: "Tool execution failed.",
                    details = data,
                )
            }
            else -> SpringboardCommandResultDto.Success(data = data)
        }
    }

    private suspend fun io.ktor.server.application.ApplicationCall.respondCommand(
        status: HttpStatusCode,
        response: SpringboardCommandResponseEnvelopeDto,
    ) {
        respondText(
            text = SpringboardCommandJson.encodeResponse(response),
            contentType = ContentType.Application.Json,
            status = status,
        )
    }

    private class DefaultCommandApiServerHandle(
        override val baseUrl: String,
        override val token: String,
        private val stopServer: () -> Unit,
        private val discoveryFile: CommandApiDiscoveryFile,
        private val shutdownHook: Thread,
    ) : CommandApiServerHandle {
        override fun stop() {
            discoveryFile.delete()
            runCatching {
                Runtime.getRuntime().removeShutdownHook(shutdownHook)
            }
            stopServer()
        }
    }

    private data class CommandApiToolRequest(
        val requestId: String?,
        val argumentsAsJsonString: String,
    )
}
