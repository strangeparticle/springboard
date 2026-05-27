package com.strangeparticle.luther.session

import com.strangeparticle.luther.toolcall.ToolCallExecutionContext

internal interface AiSessionToolCallExecutionContextFactory {
    fun createToolCallExecutionContext(
        onStateChanged: () -> Unit,
        awaitUserApproval: suspend (toolCallId: String) -> Boolean,
    ): ToolCallExecutionContext
}
