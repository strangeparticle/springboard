package com.strangeparticle.luther.session.event

internal data class ProviderModelChangedChatHistoryItem(
    val providerLabel: String,
    val modelLabel: String,
) : ChatHistoryItem {
    val displayText: String
        get() = "Active AI provider/model: $providerLabel:$modelLabel"
}
