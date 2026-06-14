package com.strangeparticle.luther.core.session

/** Observable readiness of a LutherSession, for host UI state. */
internal data class LutherStatus(
    val isReady: Boolean,
    val providerId: String?,
    val modelId: String?,
    val lastError: String?,
)
