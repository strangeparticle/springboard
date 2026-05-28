package com.strangeparticle.springboard.command

sealed interface SpringboardCommand {
    data object Status : SpringboardCommand

    data class ActivateCoordinate(
        val tabId: String? = null,
        val environmentId: String,
        val appId: String,
        val resourceId: String,
    ) : SpringboardCommand

    data class OpenSpringboard(
        val source: String,
        val inNewTab: Boolean,
    ) : SpringboardCommand

    data class SwitchTab(
        val tabIndex: Int,
    ) : SpringboardCommand

    data class ShowGuidance(
        val environmentId: String,
        val appId: String,
        val resourceId: String,
    ) : SpringboardCommand
}
