package com.strangeparticle.springboard.app.viewmodel

import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.ui.gridnav.GridZoomSelection

data class TabState(
    val tabId: String,
    val label: String,
    val source: String?,
    val springboard: Springboard?,
    val selectedEnvironmentId: String?,
    val selectedAppId: String?,
    val selectedResourceId: String?,
    val multiSelectSet: Set<Coordinate>,
    val hoveredActivatorPreview: String?,
    val gridZoomSelection: GridZoomSelection,
    val isLoading: Boolean,
) {
    val isEmpty: Boolean get() = springboard == null

    companion object {
        const val DEFAULT_EMPTY_LABEL: String = "New Tab"

        fun createEmpty(tabId: String): TabState = TabState(
            tabId = tabId,
            label = DEFAULT_EMPTY_LABEL,
            source = null,
            springboard = null,
            selectedEnvironmentId = null,
            selectedAppId = null,
            selectedResourceId = null,
            multiSelectSet = emptySet(),
            hoveredActivatorPreview = null,
            gridZoomSelection = GridZoomSelection.default(),
            isLoading = false,
        )
    }
}
