package com.strangeparticle.springboard.app.viewmodel

import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.ui.gridnav.GridZoomSelection

data class TabState(
    val tabId: String,
    val label: String,
    val source: String?,
    val springboardFilteredForRuntime: Springboard?,
    val springboardUnfiltered: Springboard?,
    val selectedEnvironmentId: String?,
    val selectedAppId: String?,
    val selectedResourceId: String?,
    val multiSelectSet: Set<Coordinate>,
    val hoveredActivatorPreview: String?,
    val gridZoomSelection: GridZoomSelection,
    val isLoading: Boolean,
    val isDirty: Boolean = false,
    val s3AwsProfile: String? = null,
    val s3LastEtag: String? = null,
) {
    val isEmpty: Boolean get() = springboardFilteredForRuntime == null

    companion object {
        const val DEFAULT_EMPTY_LABEL: String = "New Tab"

        fun createEmpty(tabId: String): TabState = TabState(
            tabId = tabId,
            label = DEFAULT_EMPTY_LABEL,
            source = null,
            springboardFilteredForRuntime = null,
            springboardUnfiltered = null,
            selectedEnvironmentId = null,
            selectedAppId = null,
            selectedResourceId = null,
            multiSelectSet = emptySet(),
            hoveredActivatorPreview = null,
            gridZoomSelection = GridZoomSelection.default(),
            isLoading = false,
            isDirty = false,
            s3AwsProfile = null,
            s3LastEtag = null,
        )
    }
}
