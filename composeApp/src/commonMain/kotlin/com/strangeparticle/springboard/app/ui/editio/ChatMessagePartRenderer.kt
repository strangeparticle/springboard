package com.strangeparticle.springboard.app.ui.editio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.editio.session.ChatMessagePart
import com.strangeparticle.editio.session.ToolCallState
import com.strangeparticle.springboard.app.ui.TestTags

@Composable
internal fun ChatMessagePartRenderer(
    part: ChatMessagePart,
    onApprovalDecision: (toolCallId: String, approved: Boolean) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        when (part) {
            is ChatMessagePart.UserText -> Text("You: ${part.text}", fontSize = 13.sp)
            is ChatMessagePart.AssistantText -> Text("Assistant: ${part.text}", fontSize = 13.sp)
            is ChatMessagePart.ChatError -> Text(
                "Error: ${part.message}",
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
            )
            is ChatMessagePart.ToolCall -> ToolCallRenderer(part, onApprovalDecision)
        }
    }
}

@Composable
private fun ToolCallRenderer(
    part: ChatMessagePart.ToolCall,
    onApprovalDecision: (toolCallId: String, approved: Boolean) -> Unit,
) {
    val toolCall = part.toolCall
    when (val state = part.state) {
        ToolCallState.Pending -> AssistChip(onClick = {}, label = { Text("Working") })
        ToolCallState.ApprovalRequested -> {
            Text("Approval requested", fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Row {
                Button(
                    onClick = { onApprovalDecision(toolCall.toolCallId, true) },
                    modifier = Modifier.testTag(TestTags.AI_APPROVAL_APPLY_BUTTON),
                ) { Text("Apply") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onApprovalDecision(toolCall.toolCallId, false) },
                    modifier = Modifier.testTag(TestTags.AI_APPROVAL_CANCEL_BUTTON),
                ) { Text("Cancel") }
            }
        }
        is ToolCallState.ApprovalResponded -> Text(if (state.approved) "Approval granted" else "Approval denied")
        is ToolCallState.OutputAvailable -> Unit
        is ToolCallState.OutputError -> Text(state.message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        ToolCallState.OutputDenied -> Text("Denied", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
    }
}
