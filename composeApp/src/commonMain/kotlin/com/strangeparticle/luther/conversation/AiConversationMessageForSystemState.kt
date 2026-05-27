package com.strangeparticle.luther.conversation

/**
 * Synthetic state-injection message. Each provider chooses how to surface this in
 * its native envelope.
 */
internal data class AiConversationMessageForSystemState(val snapshotJson: String) : AiConversationMessage()
