package com.strangeparticle.springboard.command

import com.strangeparticle.springboard.command.dto.SpringboardCommandDto
import com.strangeparticle.springboard.command.dto.SpringboardCommandDtoMapper
import com.strangeparticle.springboard.command.dto.SpringboardCommandRequestEnvelopeDto
import com.strangeparticle.springboard.command.dto.SpringboardCommandResponseEnvelopeDto
import com.strangeparticle.springboard.command.dto.SpringboardCommandResultDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SpringboardCommandFrameworkTest {
    @Test
    fun activateCoordinateDefinitionSupportsToolCalling() {
        val definition = SpringboardCommandCatalog.requireById("activateCoordinate")

        assertEquals("activate_coordinate", definition.toolCallName)
        assertEquals(listOf("environmentId", "appId", "resourceId"), definition.arguments.map { it.id })
        assertEquals(listOf("environment_id", "app_id", "resource_id"), definition.arguments.map { it.toolCallName })
    }

    @Test
    fun catalogIncludesInitialCommandSet() {
        assertEquals(
            listOf("status", "activateCoordinate", "openSpringboard", "switchTab", "showGuidance"),
            SpringboardCommandCatalog.definitions.map { it.id },
        )
    }

    @Test
    fun commandDtoRoundTripsThroughJson() {
        val command = SpringboardCommandDto.ActivateCoordinate(
            tabId = null,
            environmentId = "prod",
            appId = "github",
            resourceId = "repo",
        )

        val encoded = SpringboardCommandJson.encodeCommand(command)
        val decoded = SpringboardCommandJson.decodeCommand(encoded)

        assertEquals(command, decoded)
    }

    @Test
    fun requestEnvelopeRoundTripsThroughJson() {
        val envelope = SpringboardCommandRequestEnvelopeDto(
            requestId = "request-1",
            command = SpringboardCommandDto.Status,
        )

        val encoded = SpringboardCommandJson.encodeRequest(envelope)
        val decoded = SpringboardCommandJson.decodeRequest(encoded)

        assertEquals(envelope, decoded)
    }

    @Test
    fun resultDtoRoundTripsThroughJson() {
        val envelope = SpringboardCommandResponseEnvelopeDto(
            requestId = "request-1",
            result = SpringboardCommandResultDto.Failure(
                code = "coordinateNotFound",
                message = "No activator found.",
            ),
        )

        val encoded = SpringboardCommandJson.encodeResponse(envelope)
        val decoded = SpringboardCommandJson.decodeResponse(encoded)

        assertEquals(envelope, decoded)
    }

    @Test
    fun responseEnvelopeJsonIncludesProtocolVersionAndNullRequestId() {
        val envelope = SpringboardCommandResponseEnvelopeDto(
            requestId = null,
            result = SpringboardCommandResultDto.Success(
                message = "Springboard is running.",
            ),
        )

        val encoded = SpringboardCommandJson.encodeResponse(envelope)

        assertEquals(
            """{"protocolVersion":1,"requestId":null,"result":{"type":"success","message":"Springboard is running.","data":null}}""",
            encoded,
        )
    }

    @Test
    fun commandDtoMapsToSemanticCommand() {
        val command = SpringboardCommandDtoMapper.toCommand(
            SpringboardCommandDto.ActivateCoordinate(
                tabId = "tab-1",
                environmentId = "prod",
                appId = "github",
                resourceId = "repo",
            )
        )

        assertEquals(
            SpringboardCommand.ActivateCoordinate(
                tabId = "tab-1",
                environmentId = "prod",
                appId = "github",
                resourceId = "repo",
            ),
            command,
        )
    }

    @Test
    fun openSpringboardDtoMapsToSemanticCommand() {
        val command = SpringboardCommandDtoMapper.toCommand(
            SpringboardCommandDto.OpenSpringboard(
                source = "/tmp/dev.springboard.json",
                inNewTab = true,
            )
        )

        assertEquals(
            SpringboardCommand.OpenSpringboard(
                source = "/tmp/dev.springboard.json",
                inNewTab = true,
            ),
            command,
        )
    }

    @Test
    fun blankActivateCoordinateArgumentMapsToValidationFailure() {
        val result = SpringboardCommandDtoMapper.toCommandResult(
            SpringboardCommandDto.ActivateCoordinate(
                tabId = null,
                environmentId = " ",
                appId = "github",
                resourceId = "repo",
            )
        )

        val failure = assertIs<SpringboardCommandResult.Failure>(result)
        assertEquals(SpringboardCommandErrorCode.ValidationFailed, failure.code)
    }
}
