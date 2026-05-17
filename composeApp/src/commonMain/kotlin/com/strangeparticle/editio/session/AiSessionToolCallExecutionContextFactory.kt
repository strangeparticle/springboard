package com.strangeparticle.editio.session

import com.strangeparticle.editio.toolcall.ToolCallExecutionContext

internal interface AiSessionToolCallExecutionContextFactory {
    fun createToolCallExecutionContext(
        onStateChanged: () -> Unit,
        awaitUserApproval: suspend (toolCallId: String) -> Boolean,
    ): ToolCallExecutionContext
}
