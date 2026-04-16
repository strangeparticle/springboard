package com.strangeparticle.springboard.app.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.strangeparticle.springboard.app.domain.factory.SpringboardFactory
import com.strangeparticle.springboard.app.domain.model.*
import com.strangeparticle.springboard.app.loading.SpringboardLoader
import com.strangeparticle.springboard.app.platform.PlatformActivationService
import com.strangeparticle.springboard.app.platform.PlatformActivationServiceDefaultImpl
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.ui.gridnav.GridZoomSelection
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster

class SpringboardViewModel(
    private val settingsManager: SettingsManager,
    private val platformActivationService: PlatformActivationService = PlatformActivationServiceDefaultImpl(),
) : ViewModel() {

    var springboard by mutableStateOf<Springboard?>(null)
        private set

    var selectedEnvironmentId by mutableStateOf<String?>(null)
        private set

    var selectedAppId by mutableStateOf<String?>(null)
        private set

    var selectedResourceId by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var multiSelectSet by mutableStateOf<Set<Coordinate>>(emptySet())
        private set

    /** Activator preview text shown when hovering a cell or when keyNav fully selects a coordinate. */
    var hoveredActivatorPreview by mutableStateOf<String?>(null)

    var gridZoomSelection by mutableStateOf<GridZoomSelection>(GridZoomSelection.FixedZoom(100))

    /**
     * UI flag indicating the app dropdown should take focus. KeyNav observes this and resets it
     * to false after requesting focus. Set via [requestFocusAppDropdown] from anywhere in the app
     * (view model, toast overlay, platform window-focus hooks, etc.) without threading a
     * FocusRequester through the composable tree.
     */
    var focusAppDropdownRequested by mutableStateOf(false)

    val environments by derivedStateOf { springboard?.environments ?: emptyList() }
    val apps by derivedStateOf { springboard?.apps ?: emptyList() }
    val resources by derivedStateOf { springboard?.resources ?: emptyList() }

    val isConfigLoaded by derivedStateOf { springboard != null }

    val appEnabledStates by derivedStateOf {
        val currentSpringboard = springboard ?: return@derivedStateOf emptyMap<String, Boolean>()
        currentSpringboard.apps.associate { app ->
            app.id to hasMatchingActivator(
                environmentId = selectedEnvironmentId,
                appId = app.id,
                resourceId = selectedResourceId,
            )
        }
    }

    val resourceEnabledStates by derivedStateOf {
        val currentSpringboard = springboard ?: return@derivedStateOf emptyMap<String, Boolean>()
        currentSpringboard.resources.associate { resource ->
            resource.id to hasMatchingActivator(
                environmentId = selectedEnvironmentId,
                appId = selectedAppId,
                resourceId = resource.id,
            )
        }
    }

    val environmentEnabledStates by derivedStateOf {
        val currentSpringboard = springboard ?: return@derivedStateOf emptyMap<String, Boolean>()
        currentSpringboard.environments.associate { environment ->
            environment.id to hasMatchingActivator(
                environmentId = environment.id,
                appId = selectedAppId,
                resourceId = selectedResourceId,
            )
        }
    }

    val keyNavCoordinate by derivedStateOf {
        val environmentId = selectedEnvironmentId ?: return@derivedStateOf null
        val appId = selectedAppId ?: return@derivedStateOf null
        val resourceId = selectedResourceId ?: return@derivedStateOf null
        Coordinate(environmentId, appId, resourceId)
    }

    val isActivateEnabled by derivedStateOf {
        val environmentId = selectedEnvironmentId ?: return@derivedStateOf false
        val appId = selectedAppId ?: return@derivedStateOf false
        val resourceId = selectedResourceId ?: return@derivedStateOf false
        val currentSpringboard = springboard ?: return@derivedStateOf false
        val coordinate = Coordinate(environmentId, appId, resourceId)
        currentSpringboard.indexes.activatorByCoordinate.containsKey(coordinate)
    }

    fun loadConfig(jsonString: String, source: String) {
        try {
            isLoading = true
            val springboardConfig = SpringboardFactory.fromJson(jsonString, source)
            applySpringboard(springboardConfig)
        } catch (e: Exception) {
            ToastBroadcaster.error("Failed to load config: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    fun loadFromSource(loader: SpringboardLoader, source: String): Boolean {
        return try {
            isLoading = true
            val springboardConfig = loader.load(source)
            if (springboardConfig != null) {
                applySpringboard(springboardConfig)
                true
            } else {
                ToastBroadcaster.error("Failed to load: file not found")
                false
            }
        } catch (e: Exception) {
            ToastBroadcaster.error("Failed to load config: ${e.message}")
            false
        } finally {
            isLoading = false
        }
    }

    private fun defaultEnvironmentId(): String? {
        return springboard?.environments?.firstOrNull()?.id
    }

    fun requestFocusAppDropdown() {
        focusAppDropdownRequested = true
    }

    private fun applySpringboard(springboardConfig: Springboard) {
        springboard = springboardConfig

        selectedEnvironmentId = defaultEnvironmentId()
        selectedAppId = null
        selectedResourceId = null
        multiSelectSet = emptySet()

        val hasUnsafeActivators = springboardConfig.activators.any { it is UrlTemplateActivator || it is CommandActivator }
        if (hasUnsafeActivators) {
            ToastBroadcaster.warning(
                "This Springboard contains activators that execute CLI commands or process template expressions. Be sure you trust it before using."
            )
        }

        ToastBroadcaster.info("Springboard loaded: ${springboardConfig.name}")
        println("[Springboard] config loaded: ${springboardConfig.name}")

        requestFocusAppDropdown()
    }

    fun selectEnvironment(environmentId: String?) {
        selectedEnvironmentId = environmentId
    }

    fun selectApp(appId: String?) {
        selectedAppId = appId
    }

    fun selectResource(resourceId: String?) {
        selectedResourceId = resourceId
    }

    /** Activated via keyNav (drop-down selection + enter). */
    fun activateCurrentSelection() {
        val environmentId = selectedEnvironmentId ?: return
        val appId = selectedAppId ?: return
        val resourceId = selectedResourceId ?: return
        val currentSpringboard = springboard ?: return

        val coordinate = Coordinate(environmentId, appId, resourceId)
        val activator = currentSpringboard.indexes.activatorByCoordinate[coordinate] ?: return

        executeActivators(listOf(activator), isSingleSelection = true)
        if (settingsManager.getBoolean(SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION)) {
            resetKeyNavSelections()
        }
    }

    /** Activated via grid-nav (cell click). */
    fun activateCell(coordinate: Coordinate) {
        val currentSpringboard = springboard ?: return
        val activator = currentSpringboard.indexes.activatorByCoordinate[coordinate] ?: return
        executeActivators(listOf(activator), isSingleSelection = true)
        if (settingsManager.getBoolean(SettingsKey.RESET_KEY_NAV_AFTER_GRID_NAV_ACTIVATION)) {
            resetKeyNavSelections()
        }
    }

    /** Activated via grid-nav (column header click). */
    fun activateColumn(appId: String) {
        val environmentId = selectedEnvironmentId ?: return
        val currentSpringboard = springboard ?: return
        val activators = buildList {
            currentSpringboard.resources.forEach { resource ->
            val coordinate = Coordinate(environmentId, appId, resource.id)
            val activator = currentSpringboard.indexes.activatorByCoordinate[coordinate]
            if (activator != null) {
                    add(activator)
                }
            }
        }
        executeActivators(activators, isSingleSelection = false)
        if (settingsManager.getBoolean(SettingsKey.RESET_KEY_NAV_AFTER_GRID_NAV_ACTIVATION)) {
            resetKeyNavSelections()
        }
    }

    /** Activated via grid-nav (row label click). */
    fun activateRow(resourceId: String) {
        val environmentId = selectedEnvironmentId ?: return
        val currentSpringboard = springboard ?: return
        val activators = buildList {
            currentSpringboard.apps.forEach { app ->
            val coordinate = Coordinate(environmentId, app.id, resourceId)
            val activator = currentSpringboard.indexes.activatorByCoordinate[coordinate]
            if (activator != null) {
                    add(activator)
                }
            }
        }
        executeActivators(activators, isSingleSelection = false)
        if (settingsManager.getBoolean(SettingsKey.RESET_KEY_NAV_AFTER_GRID_NAV_ACTIVATION)) {
            resetKeyNavSelections()
        }
    }

    fun toggleMultiSelect(coordinate: Coordinate) {
        multiSelectSet = if (coordinate in multiSelectSet) {
            multiSelectSet - coordinate
        } else {
            multiSelectSet + coordinate
        }
    }

    /** Activated via grid-nav (shift-release after multi-select). */
    fun activateMultiSelect() {
        val currentSpringboard = springboard ?: return
        val activators = buildList {
            multiSelectSet.forEach { coordinate ->
            val activator = currentSpringboard.indexes.activatorByCoordinate[coordinate]
            if (activator != null) {
                    add(activator)
                }
            }
        }
        executeActivators(activators, isSingleSelection = false)
        multiSelectSet = emptySet()
        if (settingsManager.getBoolean(SettingsKey.RESET_KEY_NAV_AFTER_GRID_NAV_ACTIVATION)) {
            resetKeyNavSelections()
        }
    }

    fun getActivatorForCell(coordinate: Coordinate): Activator? {
        val currentSpringboard = springboard ?: return null
        return currentSpringboard.indexes.activatorByCoordinate[coordinate]
    }

    private fun executeActivator(activator: Activator) {
        try {
            when (activator) {
                is UrlActivator -> platformActivationService.openUrl(activator.url)
                is UrlTemplateActivator -> {
                    ToastBroadcaster.info("URL Template activators will be supported in Phase 2.")
                }
                is CommandActivator -> {
                    platformActivationService.executeCommand(activator.commandTemplate)
                }
            }
        } catch (e: Exception) {
            ToastBroadcaster.error("An error has occurred: ${e.message}")
        }
    }

    private fun executeActivators(activators: List<Activator>, isSingleSelection: Boolean) {
        if (activators.isEmpty()) return

        val urlActivators = activators.filterIsInstance<UrlActivator>()
        if (urlActivators.isNotEmpty()) {
            val shouldOpenNewWindow = if (isSingleSelection) {
                settingsManager.getBoolean(SettingsKey.OPEN_URLS_IN_NEW_WINDOW_SINGLE)
            } else {
                settingsManager.getBoolean(SettingsKey.OPEN_URLS_IN_NEW_WINDOW_MULTIPLE)
            }
            if (shouldOpenNewWindow) {
                platformActivationService.openNewBrowserWindowIfAppropriate()
            }
            platformActivationService.openUrls(urlActivators.map { it.url })
        }

        // Non-URL activators (commands) are executed individually.
        activators.forEach { activator ->
            if (activator !is UrlActivator) {
                executeActivator(activator)
            }
        }
    }

    fun resetKeyNavSelections() {
        springboard ?: return
        selectedEnvironmentId = defaultEnvironmentId()
        selectedAppId = null
        selectedResourceId = null
    }

    private fun hasMatchingActivator(
        environmentId: String?,
        appId: String?,
        resourceId: String?,
    ): Boolean {
        val currentSpringboard = springboard ?: return false
        return currentSpringboard.indexes.activatorByCoordinate.keys.any { coordinate ->
            (environmentId == null || coordinate.environmentId == environmentId) &&
                (appId == null || coordinate.appId == appId) &&
                (resourceId == null || coordinate.resourceId == resourceId)
        }
    }
}
