package com.strangeparticle.editio.conversation

/**
 * Synthetic state-injection message. Each provider chooses how to surface this in
 * its native envelope.
 */
internal data class AiClientMessageForSystemState(val snapshotJson: String) : AiClientMessage()
