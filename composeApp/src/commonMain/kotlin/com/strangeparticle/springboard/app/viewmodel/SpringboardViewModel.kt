package com.strangeparticle.springboard.app.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.strangeparticle.springboard.app.domain.factory.SpringboardFactory
import com.strangeparticle.springboard.app.domain.model.*
import com.strangeparticle.springboard.app.loading.SpringboardLoader
import com.strangeparticle.springboard.app.persistence.PersistenceService
import com.strangeparticle.springboard.app.persistence.buildTabsDto
import com.strangeparticle.springboard.app.platform.PlatformActivationService
import com.strangeparticle.springboard.app.platform.PlatformActivationServiceDefaultImpl
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.ui.gridnav.GridZoomSelection
import com.strangeparticle.springboard.app.ui.toast.TabToastState
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster

class SpringboardViewModel(
    private val settingsManager: SettingsManager,
    private val persistenceService: PersistenceService,
    private val platformActivationService: PlatformActivationService = PlatformActivationServiceDefaultImpl(),
    private val contentLoader: SpringboardContentLoader? = null,
) : ViewModel() {

    private var suppressAutosave: Boolean = false

    private var tabIdCounter = 0
    private fun generateTabId(): String = "tab-${++tabIdCounter}"

    private val _tabs = mutableStateListOf(TabState.createEmpty(generateTabId()))
    val tabs: List<TabState> get() = _tabs

    val currentTabSources: List<String>
        get() = _tabs.mapNotNull { it.source }

    private val _tabToastStates = mutableMapOf<String, TabToastState>()

    fun tabToastState(tabId: String): TabToastState =
        _tabToastStates.getOrPut(tabId) { TabToastState() }

    val activeTabToast: TabToastState
        get() = tabToastState(activeTabId)

    var activeTabId: String by mutableStateOf(_tabs.first().tabId)
        private set

    val activeTab: TabState?
        get() = _tabs.firstOrNull { it.tabId == activeTabId }

    private fun updateActiveTab(transform: (TabState) -> TabState) {
        val index = _tabs.indexOfFirst { it.tabId == activeTabId }
        if (index < 0) return
        _tabs[index] = transform(_tabs[index])
    }

    val canCreateNewTab: Boolean
        get() = _tabs.size < MAX_OPEN_TABS

    fun createTab(): String? {
        if (!canCreateNewTab) return null
        val newTab = TabState.createEmpty(generateTabId())
        _tabs.add(newTab)
        activeTabId = newTab.tabId
        onTabsChanged()
        return newTab.tabId
    }

    fun selectTab(tabId: String) {
        if (_tabs.none { it.tabId == tabId }) return
        if (activeTabId == tabId) return
        activeTabId = tabId
        onTabsChanged()
    }

    fun closeTab(tabId: String) {
        val index = _tabs.indexOfFirst { it.tabId == tabId }
        if (index < 0) return
        val wasActive = activeTabId == tabId

        if (_tabs.size == 1) {
            val replacement = TabState.createEmpty(generateTabId())
            _tabs[0] = replacement
            activeTabId = replacement.tabId
            onTabsChanged()
            return
        }

        _tabToastStates.remove(tabId)
        _tabs.removeAt(index)
        if (wasActive) {
            val nextIndex = if (index < _tabs.size) index else _tabs.size - 1
            activeTabId = _tabs[nextIndex].tabId
        }
        onTabsChanged()
    }

    fun loadConfigInNewTab(jsonString: String, source: String) {
        createTab() ?: return
        loadConfig(jsonString, source)
    }

    fun selectPreviousTab() {
        if (_tabs.size <= 1) return
        val currentIndex = _tabs.indexOfFirst { it.tabId == activeTabId }
        if (currentIndex < 0) return
        val previousIndex = if (currentIndex == 0) _tabs.size - 1 else currentIndex - 1
        selectTab(_tabs[previousIndex].tabId)
    }

    fun selectNextTab() {
        if (_tabs.size <= 1) return
        val currentIndex = _tabs.indexOfFirst { it.tabId == activeTabId }
        if (currentIndex < 0) return
        val nextIndex = if (currentIndex == _tabs.size - 1) 0 else currentIndex + 1
        selectTab(_tabs[nextIndex].tabId)
    }

    private fun onTabsChanged() {
        if (suppressAutosave) return
        try {
            persistenceService.persistTabs(buildTabsDto(_tabs.toList(), activeTabId))
        } catch (e: Exception) {
            ToastBroadcaster.error("Failed to save tabs: ${e.message}")
        }
    }

    suspend fun runSuppressingAutosave(block: suspend () -> Unit) {
        suppressAutosave = true
        try {
            block()
        } finally {
            suppressAutosave = false
            onTabsChanged()
        }
    }

    /**
     * Loads a persisted springboard into a tab as part of startup restore.
     *
     * If the only tab is the initial empty one, it is reused in place. Otherwise a new tab is
     * appended. Returns the in-memory tabId of the restored tab.
     */
    fun restoreTabFromPersistence(source: String, jsonContents: String, zoomPercent: Int): String {
        val targetIndex = if (_tabs.size == 1 && _tabs.first().isEmpty) {
            0
        } else {
            val newTab = TabState.createEmpty(generateTabId())
            _tabs.add(newTab)
            _tabs.size - 1
        }
        activeTabId = _tabs[targetIndex].tabId
        loadConfig(jsonContents, source)
        updateActiveTab { it.copy(gridZoomSelection = GridZoomSelection.fromPercent(zoomPercent)) }
        return activeTabId
    }

    var springboard: Springboard?
        get() = activeTab?.springboard
        private set(value) { updateActiveTab { it.copy(springboard = value) } }

    var selectedEnvironmentId: String?
        get() = activeTab?.selectedEnvironmentId
        private set(value) { updateActiveTab { it.copy(selectedEnvironmentId = value) } }

    var selectedAppId: String?
        get() = activeTab?.selectedAppId
        private set(value) { updateActiveTab { it.copy(selectedAppId = value) } }

    var selectedResourceId: String?
        get() = activeTab?.selectedResourceId
        private set(value) { updateActiveTab { it.copy(selectedResourceId = value) } }

    var isLoading: Boolean
        get() = activeTab?.isLoading ?: false
        private set(value) { updateActiveTab { it.copy(isLoading = value) } }

    var multiSelectSet: Set<Coordinate>
        get() = activeTab?.multiSelectSet ?: emptySet()
        private set(value) { updateActiveTab { it.copy(multiSelectSet = value) } }

    /** Activator preview text shown when hovering a cell or when keyNav fully selects a coordinate. */
    var hoveredActivatorPreview: String?
        get() = activeTab?.hoveredActivatorPreview
        set(value) { updateActiveTab { it.copy(hoveredActivatorPreview = value) } }

    var gridZoomSelection: GridZoomSelection
        get() = activeTab?.gridZoomSelection ?: GridZoomSelection.default()
        set(value) { updateActiveTab { it.copy(gridZoomSelection = value) } }

    /**
     * UI flag indicating the app dropdown should take focus. KeyNav observes this and resets it
     * to false after requesting focus. Set via [requestFocusAppDropdown] from anywhere in the app
     * (view model, toast overlay, platform window-focus hooks, etc.) without threading a
     * FocusRequester through the composable tree.
     */
    var focusAppDropdownRequested by mutableStateOf(false)

    val environments by derivedStateOf { springboard?.environments ?: emptyList() }
    val apps by derivedStateOf {
        val currentSpringboard = springboard ?: return@derivedStateOf emptyList()
        currentSpringboard.appColumnLayout()
            .filterIsInstance<AppColumn>()
            .map { it.app }
    }
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

    // Each env lights up when there is an activator at (this env, selectedApp,
    // selectedResource) OR at (ALL, selectedApp, selectedResource). The ALL
    // branch keeps this consistent with [keyNavCoordinate]'s ALL-envs fallback,
    // so an env that would activate via the all-envs activator is not falsely
    // shown as disabled.
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

    /**
     * Coordinate represented by the current keynav selection. App and resource must
     * both be selected for this to be non-null. The environment portion resolves in
     * this priority order:
     *
     *   1. The strict (selectedEnv, app, resource) coordinate — if it has a matching
     *      activator, that wins.
     *   2. The (ALL, app, resource) coordinate — used when an env is selected but
     *      the strict coordinate has no activator and an all-envs activator does
     *      cover the (app, resource) pair. Mirrors the dropdown-filtering rule that
     *      all-envs activators apply to every environment.
     *   3. Otherwise the strict coordinate is returned anyway, so callers can
     *      distinguish "selection incomplete" (null) from "selection complete but
     *      no matching activator" (non-null + isActivateEnabled = false).
     *
     * When no env is selected the strict coordinate uses the all-envs sentinel as
     * the env, so an all-envs activator can be previewed and activated without
     * choosing an environment.
     */
    val keyNavCoordinate by derivedStateOf {
        val appId = selectedAppId ?: return@derivedStateOf null
        val resourceId = selectedResourceId ?: return@derivedStateOf null

        val strictEnv = selectedEnvironmentId ?: ALL_ENVS_ENVIRONMENT_ID
        val strictCoordinate = Coordinate(strictEnv, appId, resourceId)

        val activators = springboard?.indexes?.activatorByCoordinate
            ?: return@derivedStateOf strictCoordinate

        if (activators.containsKey(strictCoordinate)) return@derivedStateOf strictCoordinate

        if (selectedEnvironmentId != null) {
            val allEnvsCoordinate = Coordinate(ALL_ENVS_ENVIRONMENT_ID, appId, resourceId)
            if (activators.containsKey(allEnvsCoordinate)) return@derivedStateOf allEnvsCoordinate
        }

        strictCoordinate
    }

    val isActivateEnabled by derivedStateOf {
        val coordinate = keyNavCoordinate ?: return@derivedStateOf false
        val currentSpringboard = springboard ?: return@derivedStateOf false
        currentSpringboard.indexes.activatorByCoordinate.containsKey(coordinate)
    }

    fun loadConfig(jsonString: String, source: String) {
        try {
            isLoading = true
            val springboardConfig = SpringboardFactory.fromJson(jsonString, source)
            applySpringboard(springboardConfig, source)
        } catch (e: Exception) {
            activeTabToast.error("Failed to load config: ${e.message}")
            isLoading = false
        }
    }

    /**
     * Reloads the active tab's springboard from its current source. Dispatches
     * to the configured [SpringboardContentLoader], which knows how to fetch
     * the raw JSON for any supported source kind (file path, http URL, s3 URL).
     * No-op when there is no active springboard.
     */
    suspend fun reloadCurrentSource() {
        val source = activeTab?.springboard?.source ?: return
        val loader = contentLoader
            ?: error("Cannot reload: SpringboardViewModel was constructed without a SpringboardContentLoader")
        try {
            val contents = loader.loadContent(source)
            loadConfig(contents, source)
        } catch (e: Exception) {
            activeTabToast.error("Failed to reload: ${e.message}")
        }
    }

    fun loadFromSource(loader: SpringboardLoader, source: String): Boolean {
        return try {
            isLoading = true
            val springboardConfig = loader.load(source)
            if (springboardConfig != null) {
                applySpringboard(springboardConfig, source)
                true
            } else {
                activeTabToast.error("Failed to load: file not found")
                isLoading = false
                false
            }
        } catch (e: Exception) {
            activeTabToast.error("Failed to load config: ${e.message}")
            isLoading = false
            false
        }
    }

    fun requestFocusAppDropdown() {
        focusAppDropdownRequested = true
    }

    private fun applySpringboard(springboardConfig: Springboard, source: String) {
        val initialEnvironmentId = springboardConfig.environments.firstOrNull()?.id

        updateActiveTab { current ->
            current.copy(
                springboard = springboardConfig,
                source = source,
                label = deriveTabLabel(springboardConfig.name),
                selectedEnvironmentId = initialEnvironmentId,
                selectedAppId = null,
                selectedResourceId = null,
                multiSelectSet = emptySet(),
                hoveredActivatorPreview = null,
                isLoading = false,
            )
        }

        val hasUnsafeActivators = springboardConfig.activators.any { it is UrlTemplateActivator || it is CommandActivator }
        if (hasUnsafeActivators) {
            activeTabToast.warning(
                "This Springboard contains activators that execute CLI commands or process template expressions. Be sure you trust it before using."
            )
        }

        activeTabToast.info("Springboard loaded: ${springboardConfig.name}")
        println("[Springboard] config loaded: ${springboardConfig.name}")
        requestFocusAppDropdown()
        onTabsChanged()
    }

    fun selectEnvironment(environmentId: String?) {
        updateActiveTab { it.copy(selectedEnvironmentId = environmentId) }
    }

    fun selectApp(appId: String?) {
        updateActiveTab { it.copy(selectedAppId = appId) }
    }

    fun selectResource(resourceId: String?) {
        updateActiveTab { it.copy(selectedResourceId = resourceId) }
    }

    /** Activated via keyNav (drop-down selection + enter). */
    fun activateCurrentSelection() {
        val coordinate = keyNavCoordinate ?: return
        val currentSpringboard = springboard ?: return
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

    /**
     * Activated via grid-nav (column header click). The environmentId identifies which
     * grid section the click came from — the env-specific section uses the selected env,
     * the all-envs section uses the all-envs env id.
     *
     * Per-resource lookup is all-envs-aware: when a non-ALL env is given and the strict
     * (env, app, resource) coordinate has no activator, falls back to (ALL, app, resource).
     * Mirrors the same fallback used by [keyNavCoordinate], so column-header clicks in an
     * env-specific section also fire all-envs activators that apply to the column.
     */
    fun activateColumn(environmentId: String, appId: String) {
        val currentSpringboard = springboard ?: return
        val activators = buildList {
            currentSpringboard.resources.forEach { resource ->
                val activator = findActivatorWithAllEnvsFallback(environmentId, appId, resource.id)
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

    /**
     * Activated via grid-nav (row label click). The environmentId identifies which grid
     * section the click came from — the env-specific section uses the selected env, the
     * all-envs section uses the all-envs env id.
     *
     * Per-app lookup is all-envs-aware: when a non-ALL env is given and the strict
     * (env, app, resource) coordinate has no activator, falls back to (ALL, app, resource).
     * Mirrors the same fallback used by [keyNavCoordinate], so row-label clicks in an
     * env-specific section also fire all-envs activators that apply to the row.
     */
    fun activateRow(environmentId: String, resourceId: String) {
        val currentSpringboard = springboard ?: return
        val activators = buildList {
            currentSpringboard.apps.forEach { app ->
                val activator = findActivatorWithAllEnvsFallback(environmentId, app.id, resourceId)
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

    /**
     * Resolves an activator for a single (environmentId, appId, resourceId) coordinate
     * with the all-envs fallback. Strict coordinate wins; when env != ALL and the strict
     * coordinate has no activator, the (ALL, app, resource) coordinate is consulted.
     * Returns null when neither yields an activator.
     */
    private fun findActivatorWithAllEnvsFallback(
        environmentId: String,
        appId: String,
        resourceId: String,
    ): Activator? {
        val activators = springboard?.indexes?.activatorByCoordinate ?: return null
        val strictCoordinate = Coordinate(environmentId, appId, resourceId)
        activators[strictCoordinate]?.let { return it }
        if (environmentId != ALL_ENVS_ENVIRONMENT_ID) {
            val allEnvsCoordinate = Coordinate(ALL_ENVS_ENVIRONMENT_ID, appId, resourceId)
            return activators[allEnvsCoordinate]
        }
        return null
    }

    fun toggleMultiSelect(coordinate: Coordinate) {
        val current = multiSelectSet
        multiSelectSet = if (coordinate in current) current - coordinate else current + coordinate
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
                    activeTabToast.info("URL Template activators will be supported in Phase 2.")
                }
                is CommandActivator -> {
                    platformActivationService.executeCommand(activator.commandTemplate) { errorMessage ->
                        activeTabToast.error(errorMessage)
                    }
                }
            }
        } catch (e: Exception) {
            activeTabToast.error("An error has occurred: ${e.message}")
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

        if (settingsManager.getBoolean(SettingsKey.HIDE_APP_AFTER_ACTIVATION)) {
            platformActivationService.hideApplicationViaPid()
        }
    }

    fun resetKeyNavSelections() {
        val currentSpringboard = springboard ?: return
        val environmentId = currentSpringboard.environments.firstOrNull()?.id
        updateActiveTab {
            it.copy(
                selectedEnvironmentId = environmentId,
                selectedAppId = null,
                selectedResourceId = null,
            )
        }
    }

    /**
     * Returns true if any activator's coordinate matches the given filter.
     * A null on `environmentId`, `appId`, or `resourceId` means "any value" for
     * that field. The env match is all-envs-aware: when a non-null env is given,
     * a coordinate also matches if its env equals [ALL_ENVS_ENVIRONMENT_ID]
     * (the all-envs sentinel applies to every environment).
     */
    private fun hasMatchingActivator(
        environmentId: String?,
        appId: String?,
        resourceId: String?,
    ): Boolean {
        val currentSpringboard = springboard ?: return false
        return currentSpringboard.indexes.activatorByCoordinate.keys.any { coordinate ->
            (environmentId == null ||
                coordinate.environmentId == environmentId ||
                coordinate.environmentId == ALL_ENVS_ENVIRONMENT_ID) &&
                (appId == null || coordinate.appId == appId) &&
                (resourceId == null || coordinate.resourceId == resourceId)
        }
    }
}
