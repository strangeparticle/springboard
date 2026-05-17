package com.strangeparticle.springboard.app.ui.editio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.ui.TestTags

@Composable
internal fun AiChatPane(
    state: AiChatPaneState,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var inputText by remember { mutableStateOf("") }
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).testTag(TestTags.AI_CHAT_PANE),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Assistant", fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(
                    text = if (state.isConfigured) "${state.providerLabel} ${state.modelLabel}" else "Not configured",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = onClose, modifier = Modifier.testTag(TestTags.AI_CHAT_CLOSE_BUTTON)) {
                    Icon(Icons.Default.Close, contentDescription = "Close assistant")
                }
            }

            if (!state.isConfigured) {
                Text("AI is not configured", fontSize = 13.sp)
                Button(onClick = onOpenSettings, modifier = Modifier.testTag(TestTags.AI_CHAT_SETTINGS_BUTTON)) {
                    Text("Open AI settings")
                }
                return@Column
            }

            Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                state.transcriptParts.forEach { part ->
                    ChatMessagePartRenderer(part = part, onApprovalDecision = state.onApprovalDecision)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f).testTag(TestTags.AI_CHAT_INPUT),
                    minLines = 1,
                    maxLines = 4,
                )
                Button(
                    onClick = {
                        val text = inputText.trim()
                        if (text.isNotEmpty()) {
                            state.onSubmit(text)
                            inputText = ""
                        }
                    },
                    enabled = !state.isRunning,
                    modifier = Modifier.testTag(TestTags.AI_CHAT_SEND_BUTTON),
                ) { Text("Send") }
                OutlinedButton(
                    onClick = state.onStop,
                    enabled = state.isRunning,
                    modifier = Modifier.testTag(TestTags.AI_CHAT_STOP_BUTTON),
                ) { Text("Stop") }
            }
        }
    }
}
