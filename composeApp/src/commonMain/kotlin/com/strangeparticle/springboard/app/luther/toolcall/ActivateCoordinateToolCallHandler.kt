package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallHandler
import com.strangeparticle.luther.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.luther.toolcall.requestSchema
import com.strangeparticle.springboard.app.luther.*
import com.strangeparticle.springboard.command.SpringboardCommand
import com.strangeparticle.springboard.command.SpringboardCommandErrorCode
import com.strangeparticle.springboard.command.SpringboardCommandResult

internal class ActivateCoordinateToolCallHandler : ToolCallHandler {
    override val providerToolId = "activate_coordinate"
    override val description = "Activate one entry by (environment_id, app_id, resource_id) — opens its URL or runs its command. Pass the tab the user named, or the active tab's id from the latest snapshot."
    override val schema = requestSchema(ActivateCoordinateToolCallHandlerRequest.serializer())

    suspend fun executeToolCallHandler(args: ActivateCoordinateToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        return when (
            val result = context.commandExecutor.execute(
                SpringboardCommand.ActivateCoordinate(
                    tabId = args.tab_id,
                    environmentId = args.environment_id,
                    appId = args.app_id,
                    resourceId = args.resource_id,
                )
            )
        ) {
            is SpringboardCommandResult.Success -> successStatusResult(result.message)
            is SpringboardCommandResult.Failure -> errorStatusResult(
                message = result.message,
                code = result.code.toActivateCoordinateToolCode(),
            )
        }
    }

    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, ActivateCoordinateToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }

    private fun SpringboardCommandErrorCode.toActivateCoordinateToolCode(): String = when (this) {
        SpringboardCommandErrorCode.TabNotFound -> "missing_tab"
        SpringboardCommandErrorCode.SpringboardNotLoaded -> "tab_empty"
        SpringboardCommandErrorCode.CoordinateNotFound -> "no_activators_resolved"
        else -> wireValue
    }
}
