package com.strangeparticle.springboard.app.ui

object TestTags {
    const val SPRINGBOARD_ICON = "springboardIcon"
    const val VERSION_DESIGNATOR = "versionDesignator"
    const val STATUS_BAR_SOURCE = "statusBarSource"
    const val RELOAD_BUTTON = "reloadButton"
    const val APP_DROPDOWN = "appDropdown"
    const val RESOURCE_DROPDOWN = "resourceDropdown"
    const val ENVIRONMENT_DROPDOWN = "environmentDropdown"
    const val TOAST_MESSAGE = "toastMessage"
    const val TOAST_SEVERITY_LABEL = "toastSeverityLabel"
    const val TOAST_DISMISS_BUTTON = "toastDismissButton"
    const val GRID_ENVIRONMENT_TITLE = "gridEnvironmentTitle"

    fun gridCell(appId: String, resourceId: String) = "gridCell_${appId}_${resourceId}"
    fun gridCellActivatorIndicator(appId: String, resourceId: String) = "gridCellActivator_${appId}_${resourceId}"
    fun gridRowLabel(resourceId: String) = "gridRowLabel_${resourceId}"
}
