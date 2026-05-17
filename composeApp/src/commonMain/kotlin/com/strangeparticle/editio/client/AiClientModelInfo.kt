package com.strangeparticle.editio.client

/** A model surfaced by [AiClient.listModels], suitable for showing in a settings dropdown. */
internal data class AiClientModelInfo(
    val id: String,
    val displayName: String?,
    val supportsToolCalling: Boolean,
)