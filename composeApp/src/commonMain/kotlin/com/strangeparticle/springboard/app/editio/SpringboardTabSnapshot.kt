package com.strangeparticle.springboard.app.editio

import com.strangeparticle.springboard.app.domain.factory.dto.SpringboardDto
import kotlinx.serialization.Serializable

/** One tab inside a [SpringboardAppSnapshot]. */
@Serializable
internal data class SpringboardTabSnapshot(
    val tabId: String,
    val label: String,
    val source: String?,
    val isDirty: Boolean,
    val springboard: SpringboardDto?,
)
