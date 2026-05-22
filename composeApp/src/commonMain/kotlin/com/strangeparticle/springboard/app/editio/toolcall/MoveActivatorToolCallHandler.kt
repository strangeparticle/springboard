package com.strangeparticle.springboard.app.editio.toolcall

import com.strangeparticle.editio.toolcall.ToolCallExecutionContext
import com.strangeparticle.editio.toolcall.ToolCallHandler
import com.strangeparticle.editio.toolcall.ToolCallHandlerResponse
import com.strangeparticle.editio.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.editio.toolcall.requestSchema
import com.strangeparticle.springboard.app.editio.*

import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.domain.mutator.SpringboardMutationError
import com.strangeparticle.springboard.app.domain.mutator.addActivator
import com.strangeparticle.springboard.app.domain.mutator.removeActivator

internal class MoveActivatorToolCallHandler : ToolCallHandler {
    override val providerToolId = "move_activator"
    override val description = "Move an activator from one loaded tab to another."
    override val schema = requestSchema(MoveActivatorToolCallHandlerRequest.serializer())
    suspend fun executeToolCallHandler(args: MoveActivatorToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        if (args.from_tab_id == args.to_tab_id) {
            return context.errorResult("Source and destination tabs must be different.", "same_tab")
        }
        val sourceTab = context.viewModel.findTab(args.from_tab_id)
            ?: return context.errorResult("No tab with id '${args.from_tab_id}'.", "missing_tab")
        val destTab = context.viewModel.findTab(args.to_tab_id)
            ?: return context.errorResult("No tab with id '${args.to_tab_id}'.", "missing_tab")
        val sourceSpringboard = sourceTab.springboardUnfiltered
            ?: return context.errorResult("Tab '${args.from_tab_id}' has no loaded springboard.", "tab_empty")
        val destSpringboard = destTab.springboardUnfiltered
            ?: return context.errorResult("Tab '${args.to_tab_id}' has no loaded springboard.", "tab_empty")
        val coord = Coordinate(args.environment_id, args.app_id, args.resource_id)
        val activator = sourceSpringboard.indexes.activatorByCoordinate[coord]
            ?: return context.errorResult("No activator at the source coordinate.", "missing_target")

        val destSpringboardWithActivator = try {
            addActivator(destSpringboard, activator)
        } catch (e: SpringboardMutationError) {
            return context.errorResult(e)
        }
        val sourceSpringboardWithoutActivator = try {
            removeActivator(sourceSpringboard, coord)
        } catch (e: SpringboardMutationError) {
            return context.errorResult(e)
        }

        context.viewModel.replaceTabSpringboard(args.from_tab_id, sourceSpringboardWithoutActivator)
        context.viewModel.markTabDirty(args.from_tab_id)
        context.viewModel.replaceTabSpringboard(args.to_tab_id, destSpringboardWithActivator)
        context.viewModel.markTabDirty(args.to_tab_id)
        context.markStateChanged()
        return context.successResult()
    }
    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, MoveActivatorToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
