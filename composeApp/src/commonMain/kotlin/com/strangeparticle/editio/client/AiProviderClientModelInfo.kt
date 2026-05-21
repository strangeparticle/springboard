package com.strangeparticle.editio.client

/** A model surfaced by [AiProviderClient.listModels], suitable for showing in a settings dropdown. */
internal data class AiProviderClientModelInfo(
    val id: String,
    val displayName: String?,
    val supportsToolCalling: Boolean,
)