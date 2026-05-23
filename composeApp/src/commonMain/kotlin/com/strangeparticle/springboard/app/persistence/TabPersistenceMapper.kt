package com.strangeparticle.springboard.app.persistence

import com.strangeparticle.springboard.app.ui.gridnav.percent
import com.strangeparticle.springboard.app.viewmodel.TabState

fun buildTabsDto(tabs: List<TabState>, activeTabId: String): TabsDto {
    val persistedTabs = tabs
        .filter { it.source != null }
        .map { tab ->
            TabDto(
                tabId = tab.tabId,
                source = tab.source,
                zoomPercent = tab.gridZoomSelection.percent,
                s3AwsProfile = tab.s3AwsProfile,
                s3LastEtag = tab.s3LastEtag,
            )
        }
    val persistedActiveTabId = if (persistedTabs.any { it.tabId == activeTabId }) {
        activeTabId
    } else {
        null
    }
    return TabsDto(tabs = persistedTabs, activeTabId = persistedActiveTabId)
}
