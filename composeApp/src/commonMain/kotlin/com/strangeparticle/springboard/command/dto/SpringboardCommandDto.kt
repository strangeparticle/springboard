package com.strangeparticle.springboard.command.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface SpringboardCommandDto {
    @Serializable
    @SerialName("status")
    data object Status : SpringboardCommandDto

    @Serializable
    @SerialName("activateCoordinate")
    data class ActivateCoordinate(
        val tabId: String? = null,
        val environmentId: String,
        val appId: String,
        val resourceId: String,
    ) : SpringboardCommandDto

    @Serializable
    @SerialName("openSpringboard")
    data class OpenSpringboard(
        val source: String,
        val inNewTab: Boolean = false,
    ) : SpringboardCommandDto

    @Serializable
    @SerialName("switchTab")
    data class SwitchTab(
        val tabIndex: Int,
    ) : SpringboardCommandDto

    @Serializable
    @SerialName("showGuidance")
    data class ShowGuidance(
        val environmentId: String,
        val appId: String,
        val resourceId: String,
    ) : SpringboardCommandDto
}
