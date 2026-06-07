package com.strangeparticle.springboard.app.unit

import com.strangeparticle.luther.session.ChatHistoryGroup
import com.strangeparticle.luther.session.ChatHistoryGroupType
import com.strangeparticle.luther.session.event.ProviderModelChangedChatHistoryItem
import com.strangeparticle.springboard.app.ui.luther.appendProviderModelState
import com.strangeparticle.springboard.app.ui.luther.initialChatHistory
import com.strangeparticle.springboard.app.ui.luther.initialTerseHelpHistory
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ProviderModelHistoryTest {

    @Test
    fun `initial chat history records active provider model before terse help`() {
        val history = initialChatHistory(
            providerLabel = "OpenAI",
            modelLabel = "gpt-4o-mini",
        )

        assertEquals(
            ChatHistoryGroup(
                ChatHistoryGroupType.PROVIDER_MODEL_CHANGE,
                listOf(ProviderModelChangedChatHistoryItem("OpenAI", "gpt-4o-mini")),
            ),
            history.first(),
        )
        assertEquals(initialTerseHelpHistory(), history.drop(1))
    }

    @Test
    fun `appending a provider model change adds a new provider model change entry`() {
        val initial = initialChatHistory(
            providerLabel = "OpenAI",
            modelLabel = "gpt-4o-mini",
        )
        val changed = appendProviderModelState(
            groups = initial,
            providerLabel = "Anthropic",
            modelLabel = "claude-3-5-sonnet-latest",
        )

        assertEquals(
            ChatHistoryGroup(
                ChatHistoryGroupType.PROVIDER_MODEL_CHANGE,
                listOf(ProviderModelChangedChatHistoryItem("Anthropic", "claude-3-5-sonnet-latest")),
            ),
            changed.last(),
        )
        assertEquals(2, changed.count { it.type == ChatHistoryGroupType.PROVIDER_MODEL_CHANGE })
        assertEquals(initial, changed.dropLast(1))
    }
}
