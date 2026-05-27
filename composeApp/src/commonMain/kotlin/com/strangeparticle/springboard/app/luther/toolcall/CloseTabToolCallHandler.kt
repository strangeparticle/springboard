package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.toolcall.ToolCallExecutionContext
import com.strangeparticle.luther.toolcall.ToolCallHandler
import com.strangeparticle.luther.toolcall.ToolCallHandlerResponse
import com.strangeparticle.luther.toolcall.decodeToolCallHandlerRequest
import com.strangeparticle.luther.toolcall.requestSchema
import com.strangeparticle.springboard.app.luther.*

internal class CloseTabToolCallHandler : ToolCallHandler {
    override val providerToolId = "close_tab"
    override val description = "Close a tab. Fails if the tab has unsaved changes — save or discard first."
    override val schema = requestSchema(CloseTabToolCallHandlerRequest.serializer())
    suspend fun executeToolCallHandler(args: CloseTabToolCallHandlerRequest, context: SpringboardToolCallExecutionContext): SpringboardToolCallHandlerResponse {
        val tab = context.viewModel.findTab(args.tab_id)
            ?: return context.errorResult("No tab with id '${args.tab_id}'.", "missing_tab")
        if (tab.isDirty) return context.errorResult("Tab '${args.tab_id}' has unsaved changes.", "tab_dirty")
        context.viewModel.closeTab(args.tab_id)
        context.markStateChanged()
        return context.successResult()

    }
    override suspend fun executeToolCallHandler(toolCallId: String, argumentsAsJsonString: String, context: ToolCallExecutionContext): ToolCallHandlerResponse {
        val args = decodeToolCallHandlerRequest(argumentsAsJsonString, CloseTabToolCallHandlerRequest.serializer())
        val springboardContext = context.getSpringboardToolCallExecutionContextOrThrow()
        return springboardContext.handleMutationErrors { executeToolCallHandler(args, springboardContext) }
    }
}
