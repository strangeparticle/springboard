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
import com.strangeparticle.springboard.app.domain.factory.SpringboardJsonWriter
import com.strangeparticle.springboard.app.platform.PlatformActivationService
import com.strangeparticle.springboard.app.platform.PlatformActivationServiceDefaultImpl
import com.strangeparticle.springboard.app.platform.PlatformFileContentService
import com.strangeparticle.springboard.app.platform.PlatformFileContentServiceDefaultImpl
import com.strangeparticle.springboard.app.platform.S3ContentService
import com.strangeparticle.springboard.app.platform.S3GetResult
import com.strangeparticle.springboard.app.platform.S3PutResult
import com.strangeparticle.springboard.app.runtime.filterSpringboardForRuntime
import com.strangeparticle.springboard.app.settings.items.core.HideAppAfterActivationSetting
import com.strangeparticle.springboard.app.settings.items.core.OpenUrlsInNewWindowMultipleSetting
import com.strangeparticle.springboard.app.settings.items.core.OpenUrlsInNewWindowSingleSetting
import com.strangeparticle.springboard.app.settings.items.core.ResetKeyNavAfterGridNavActivationSetting
import com.strangeparticle.springboard.app.settings.items.core.ResetKeyNavAfterKeyNavActivationSetting
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.ui.gridnav.GridZoomSelection
import com.strangeparticle.springboard.app.ui.toast.TabToastState
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster

class SpringboardViewModel(
    private val settingsManager: SettingsManager,
    private val persistenceService: PersistenceService,
    private val platformActivationService: PlatformActivationService = PlatformActivationServiceDefaultImpl(),
    private val contentLoader: SpringboardContentLoader? = null,
    private val fileContentService: PlatformFileContentService = PlatformFileContentServiceDefaultImpl(),
    private val s3ContentService: S3ContentService? = null,
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
        updateTabById(activeTabId, transform)
    }

    private fun updateTabById(tabId: String, transform: (TabState) -> TabState) {
        val index = _tabs.indexOfFirst { it.tabId == tabId }
        if (index < 0) return
        _tabs[index] = transform(_tabs[index])
    }

    private fun installSpringboardInTab(
        tabId: String,
        springboardConfig: Springboard,
        tabSource: String?,
        isDirty: Boolean,
        s3AwsProfile: String? = null,
        s3LastEtag: String? = null,
    ) {
        val initialEnvironmentId = springboardConfig.environments.firstOrNull()?.id
        val filteredSpringboard = filterSpringboardForRuntime(
            springboardConfig,
            settingsManager.runtimeEnvironment,
        )
        updateTabById(tabId) { current ->
            current.copy(
                springboardFilteredForRuntime = filteredSpringboard,
                springboardUnfiltered = springboardConfig,
                source = tabSource,
                label = deriveTabLabel(springboardConfig.name),
                selectedEnvironmentId = initialEnvironmentId,
                selectedAppId = null,
                selectedResourceId = null,
                multiSelectSet = emptySet(),
                hoveredActivatorPreview = null,
                isLoading = false,
                isDirty = isDirty,
                s3AwsProfile = s3AwsProfile,
                s3LastEtag = s3LastEtag,
            )
        }
    }

    /** Returns the [TabState] for [tabId], or null if no tab with that id exists. */
    fun findTab(tabId: String): TabState? = _tabs.firstOrNull { it.tabId == tabId }

    /**
     * Marks the active tab dirty (in-memory springboard differs from its source).
     * Wired up by chat-driven mutations in Phase 2; no callers in Phase 1.
     */
    fun markActiveTabDirty() {
        updateActiveTab { it.copy(isDirty = true) }
    }

    /** Marks [tabId] dirty. Used by AI tools that target a specific (possibly inactive) tab. */
    fun markTabDirty(tabId: String) {
        updateTabById(tabId) { it.copy(isDirty = true) }
    }

    /** Clears the active tab's dirty flag (e.g. after a successful save). */
    fun clearActiveTabDirty() {
        updateActiveTab { it.copy(isDirty = false) }
    }

    /**
     * Replaces the springboard held by [tabId] with [newSpringboard]. Used by AI
     * tools after a pure-function mutator has produced a validated new springboard.
     * Does not flip the dirty flag — callers do that separately so they can keep
     * dirty-marking and state-changed signaling clean and explicit.
     */
    fun replaceTabSpringboard(tabId: String, newSpringboard: com.strangeparticle.springboard.app.domain.model.Springboard) {
        val filteredSpringboard = filterSpringboardForRuntime(
            newSpringboard,
            settingsManager.runtimeEnvironment,
        )
        updateTabById(tabId) {
            it.copy(
                springboardFilteredForRuntime = filteredSpringboard,
                springboardUnfiltered = newSpringboard,
            )
        }
    }

    /**
     * True when the active tab has a loaded springboard whose source is a local-file
     * path (and therefore can be saved in place via [saveActiveTab]). False for empty
     * tabs, HTTP(S) network-sourced tabs, and tabs with no source at all.
     */
    val canSaveActiveTabInPlace: Boolean
        get() {
            val tab = activeTab ?: return false
            if (tab.springboardUnfiltered == null) return false
            val source = tab.source ?: return false
            if (!isNonSaveableInPlaceSource(source)) return true
            return tab.s3AwsProfile != null
        }

    /**
     * Re-serializes the active tab's in-memory springboard via [SpringboardJsonWriter]
     * and writes it to the tab's existing local-file source path. Clears dirty on
     * success. Returns a [SaveResult] describing the outcome; the caller dispatches
     * user-facing feedback (toast, dialog, etc.).
     *
     * Use [saveActiveTabAs] for "save to a new location" flows. Network-sourced tabs
     * (HTTP/HTTPS) cannot be saved in place — those return [SaveResult.NotSupportedForSource].
     */
    /**
     * Re-serializes the springboard in [tabId] and writes it to that tab's existing
     * local-file source path. Clears dirty on success. Works for any tab, not only
     * the active one — no active-tab switch required.
     */
    suspend fun saveTab(tabId: String): SaveResult {
        val tab = findTab(tabId) ?: return SaveResult.NoSpringboard
        val springboardUnfiltered = tab.springboardUnfiltered ?: return SaveResult.NoSpringboard
        val source = tab.source ?: return SaveResult.NotSupportedForSource
        if (isNonSaveableInPlaceSource(source)) {
            return if (tab.s3AwsProfile != null) {
                writeSpringboardToS3(tab, source, springboardUnfiltered, ifMatch = tab.s3LastEtag)
            } else {
                SaveResult.NotSupportedForSource
            }
        }
        return writeSpringboardTo(source, springboardUnfiltered, rewriteTabSource = false, tabId = tabId)
    }

    suspend fun saveActiveTab(): SaveResult = saveTab(activeTabId)

    /**
     * Like [saveTab] but ignores any previously-captured ETag and unconditionally
     * overwrites the S3 object. Used by the modified-externally conflict dialog
     * when the user picks "Overwrite". For non-S3 tabs the behaviour is identical
     * to [saveTab].
     */
    suspend fun saveTabOverwriting(tabId: String): SaveResult {
        val tab = findTab(tabId) ?: return SaveResult.NoSpringboard
        val springboardUnfiltered = tab.springboardUnfiltered ?: return SaveResult.NoSpringboard
        val source = tab.source ?: return SaveResult.NotSupportedForSource
        if (isNonSaveableInPlaceSource(source)) {
            return if (tab.s3AwsProfile != null) {
                writeSpringboardToS3(tab, source, springboardUnfiltered, ifMatch = null)
            } else {
                SaveResult.NotSupportedForSource
            }
        }
        return writeSpringboardTo(source, springboardUnfiltered, rewriteTabSource = false, tabId = tabId)
    }

    /**
     * Re-serializes the active tab's springboard via [SpringboardJsonWriter] and writes
     * to [targetPath] (a local-file path chosen by the user via Save As). On success
     * the tab's [TabState.source] is rewritten to [targetPath] so subsequent in-place
     * saves overwrite that file. Works for any active-tab source, including HTTP/HTTPS;
     * Save As is the supported way to take a network-loaded springboard to disk.
     *
     * Save As also drops any S3 association on the tab — the new source is a local
     * path, so the s3 profile / etag from the previous source no longer apply.
     */
    fun saveActiveTabAs(targetPath: String): SaveResult {
        val tab = activeTab ?: return SaveResult.NoSpringboard
        val springboardUnfiltered = tab.springboardUnfiltered ?: return SaveResult.NoSpringboard
        return writeSpringboardTo(targetPath, springboardUnfiltered, rewriteTabSource = true, tabId = activeTabId)
    }

    private fun writeSpringboardTo(
        targetPath: String,
        springboard: com.strangeparticle.springboard.app.domain.model.Springboard,
        rewriteTabSource: Boolean,
        tabId: String,
    ): SaveResult {
        val json = SpringboardJsonWriter.toJson(springboard)
        val ok = try {
            fileContentService.writeFileContents(targetPath, json)
        } catch (e: Exception) {
            return SaveResult.WriteFailed(targetPath, e.message ?: "unknown error")
        }
        return if (ok) {
            updateTabById(tabId) { current ->
                if (rewriteTabSource) {
                    current.copy(
                        isDirty = false,
                        source = targetPath,
                        s3AwsProfile = null,
                        s3LastEtag = null,
                    )
                } else {
                    current.copy(isDirty = false)
                }
            }
            SaveResult.Success(targetPath)
        } else {
            SaveResult.WriteFailed(targetPath, "writeFileContents returned false")
        }
    }

    private suspend fun writeSpringboardToS3(
        tab: TabState,
        sourceUrl: String,
        springboard: com.strangeparticle.springboard.app.domain.model.Springboard,
        ifMatch: String?,
    ): SaveResult {
        val service = s3ContentService
            ?: return SaveResult.WriteFailed(sourceUrl, "S3 content service is not available")
        val profile = tab.s3AwsProfile
            ?: return SaveResult.NotSupportedForSource
        val json = SpringboardJsonWriter.toJson(springboard)
        return when (val outcome = service.putObject(sourceUrl, profile, json, ifMatch)) {
            is S3PutResult.Success -> {
                updateTabById(tab.tabId) { current ->
                    current.copy(isDirty = false, s3LastEtag = outcome.etag ?: current.s3LastEtag)
                }
                SaveResult.Success(sourceUrl)
            }
            is S3PutResult.Conflict -> SaveResult.Conflict(sourceUrl, outcome.message)
            is S3PutResult.Denied -> SaveResult.Denied(sourceUrl, outcome.message)
            is S3PutResult.CredentialsUnavailable -> SaveResult.WriteFailed(sourceUrl, outcome.message)
            is S3PutResult.Failed -> SaveResult.WriteFailed(sourceUrl, outcome.message)
        }
    }

    private fun isNonSaveableInPlaceSource(source: String): Boolean {
        val lowered = source.lowercase()
        return lowered.startsWith("http://") ||
            lowered.startsWith("https://")
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

    fun createUnsavedSpringboardTab(): LoadResult {
        val newTabId = createTab()
            ?: return LoadResult.Failure(
                code = "tab_limit_reached",
                message = "Cannot create a new tab — tab limit reached.",
            )
        val springboardConfig = SpringboardFactory.createEmpty(nextUntitledSpringboardName())
        installSpringboardInTab(
            tabId = newTabId,
            springboardConfig = springboardConfig,
            tabSource = null,
            isDirty = true,
        )
        activeTabToast.info("Springboard created: ${springboardConfig.name}")
        requestFocusAppDropdown()
        onTabsChanged()
        return LoadResult.Success(newTabId)
    }

    private fun nextUntitledSpringboardName(): String {
        val existingNames = _tabs.mapNotNull { it.springboardFilteredForRuntime?.name }.toSet()
        var index = 1
        while ("Untitled-$index" in existingNames) {
            index++
        }
        return "Untitled-$index"
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

    fun loadConfigInNewTab(jsonString: String, source: String, s3AwsProfile: String? = null, s3LastEtag: String? = null) {
        createTab() ?: return
        loadConfig(jsonString, source, s3AwsProfile, s3LastEtag)
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
    fun restoreTabFromPersistence(
        source: String,
        jsonContents: String,
        zoomPercent: Int,
        s3AwsProfile: String? = null,
        s3LastEtag: String? = null,
    ): String {
        val targetIndex = if (_tabs.size == 1 && _tabs.first().isEmpty) {
            0
        } else {
            val newTab = TabState.createEmpty(generateTabId())
            _tabs.add(newTab)
            _tabs.size - 1
        }
        activeTabId = _tabs[targetIndex].tabId
        loadConfig(jsonContents, source, s3AwsProfile = s3AwsProfile, s3LastEtag = s3LastEtag)
        updateActiveTab { it.copy(gridZoomSelection = GridZoomSelection.fromPercent(zoomPercent)) }
        return activeTabId
    }

    var springboardFilteredForRuntime: Springboard?
        get() = activeTab?.springboardFilteredForRuntime
        private set(value) { updateActiveTab { it.copy(springboardFilteredForRuntime = value) } }

    val springboardUnfiltered: Springboard?
        get() = activeTab?.springboardUnfiltered

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

    val environments by derivedStateOf { springboardFilteredForRuntime?.environments ?: emptyList() }
    val apps by derivedStateOf {
        val currentSpringboardFilteredForRuntime = springboardFilteredForRuntime ?: return@derivedStateOf emptyList()
        currentSpringboardFilteredForRuntime.appColumnLayout()
            .filterIsInstance<AppColumn>()
            .map { it.app }
    }
    val resources by derivedStateOf { springboardFilteredForRuntime?.resources ?: emptyList() }

    val isConfigLoaded by derivedStateOf { springboardFilteredForRuntime != null }

    val appEnabledStates by derivedStateOf {
        val currentSpringboardFilteredForRuntime = springboardFilteredForRuntime ?: return@derivedStateOf emptyMap<String, Boolean>()
        currentSpringboardFilteredForRuntime.apps.associate { app ->
            app.id to hasMatchingActivator(
                environmentId = selectedEnvironmentId,
                appId = app.id,
                resourceId = selectedResourceId,
            )
        }
    }

    val resourceEnabledStates by derivedStateOf {
        val currentSpringboardFilteredForRuntime = springboardFilteredForRuntime ?: return@derivedStateOf emptyMap<String, Boolean>()
        currentSpringboardFilteredForRuntime.resources.associate { resource ->
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
        val currentSpringboardFilteredForRuntime = springboardFilteredForRuntime ?: return@derivedStateOf emptyMap<String, Boolean>()
        currentSpringboardFilteredForRuntime.environments.associate { environment ->
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

        val activators = springboardFilteredForRuntime?.indexes?.activatorByCoordinate
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
        val currentSpringboardFilteredForRuntime = springboardFilteredForRuntime ?: return@derivedStateOf false
        currentSpringboardFilteredForRuntime.indexes.activatorByCoordinate.containsKey(coordinate)
    }

    fun loadConfig(jsonString: String, source: String, s3AwsProfile: String? = null, s3LastEtag: String? = null) {
        try {
            isLoading = true
            val springboardConfig = SpringboardFactory.fromJson(jsonString, source)
            applySpringboard(springboardConfig, source, s3AwsProfile, s3LastEtag)
        } catch (e: Exception) {
            activeTabToast.error("Failed to load config: ${e.message}")
            isLoading = false
        }
    }

    /**
     * Reloads the active tab's springboard from its current source. Dispatches
     * to the configured [SpringboardContentLoader], which knows how to fetch
     * the raw JSON for supported source kinds (file path or http/https URL).
     * No-op when there is no active springboard.
     */
    suspend fun reloadCurrentSource() {
        val source = activeTab?.springboardFilteredForRuntime?.source ?: return
        val loader = contentLoader
        if (loader == null) {
            activeTabToast.error("Failed to reload: SpringboardViewModel was constructed without a SpringboardContentLoader")
            return
        }
        try {
            val contents = loader.loadContent(source)
            loadConfig(contents, source)
        } catch (e: Exception) {
            activeTabToast.error("Failed to reload: ${e.message}")
        }
    }

    /**
     * Outcome of [loadConfigFromSource]. Carries the loaded tab id on success and a
     * structured error code/message on failure so AI tool callers can surface the
     * problem in their tool result.
     */
    sealed class LoadResult {
        data class Success(val tabId: String) : LoadResult()
        data class Failure(val code: String, val message: String) : LoadResult()
    }

    /**
     * Load a springboard from [source] (file path or URL) via the configured
     * [SpringboardContentLoader]. When [inNewTab] is true, creates a new tab first
     * and loads into it; otherwise loads into the currently active tab (replacing
     * any existing springboard there). Used by AI tab-management tools.
     */
    suspend fun loadConfigFromSource(source: String, inNewTab: Boolean): LoadResult {
        val loader = contentLoader
            ?: return LoadResult.Failure(
                code = "loader_not_configured",
                message = "Cannot load: SpringboardViewModel was constructed without a SpringboardContentLoader.",
            )

        if (inNewTab) {
            val newTabId = createTab()
                ?: return LoadResult.Failure(
                    code = "tab_limit_reached",
                    message = "Cannot create a new tab — tab limit reached.",
                )
            val result = loadIntoActive(loader, source, newTabId)
            if (result is LoadResult.Failure) closeTab(newTabId)
            return result
        }
        return loadIntoActive(loader, source, activeTabId)
    }

    /**
     * Load a springboard from the S3 [url] authenticated with the named AWS CLI
     * [profile]. When [inNewTab] is true creates a new tab first and loads into
     * it. The tab's S3 association (profile + last ETag) is recorded so
     * subsequent in-place saves PUT back to the same object with If-Match
     * conflict detection.
     */
    suspend fun loadConfigFromS3(url: String, profile: String, inNewTab: Boolean): LoadResult {
        val service = s3ContentService
            ?: return LoadResult.Failure(
                code = "s3_not_configured",
                message = "Cannot load: SpringboardViewModel was constructed without an S3ContentService.",
            )

        val targetTabId = if (inNewTab) {
            createTab() ?: return LoadResult.Failure(
                code = "tab_limit_reached",
                message = "Cannot create a new tab — tab limit reached.",
            )
        } else {
            activeTabId
        }

        val result = when (val outcome = service.getObject(url, profile)) {
            is S3GetResult.Success -> {
                try {
                    val springboardConfig = SpringboardFactory.fromJson(outcome.content, url)
                    installSpringboardInTab(
                        tabId = targetTabId,
                        springboardConfig = springboardConfig,
                        tabSource = url,
                        isDirty = false,
                        s3AwsProfile = profile,
                        s3LastEtag = outcome.etag,
                    )
                    val hasUnsafeActivators = hasUnsafeActivatorsForRuntime(springboardConfig)
                    if (hasUnsafeActivators) {
                        tabToastState(targetTabId).warning(
                            "This Springboard contains activators that execute CLI commands or process template expressions. Be sure you trust it before using."
                        )
                    }
                    tabToastState(targetTabId).info("Loaded from S3: ${springboardConfig.name}")
                    if (targetTabId == activeTabId) requestFocusAppDropdown()
                    onTabsChanged()
                    LoadResult.Success(targetTabId)
                } catch (e: Exception) {
                    LoadResult.Failure(
                        code = "parse_failed",
                        message = "Failed to parse springboard at '$url': ${e.message ?: "unknown error"}",
                    )
                }
            }
            is S3GetResult.Denied -> LoadResult.Failure(
                code = "s3_denied",
                message = "Open from S3 failed — your AWS profile doesn't have read permission. ${outcome.message}",
            )
            is S3GetResult.CredentialsUnavailable -> LoadResult.Failure(
                code = "s3_credentials_unavailable",
                message = "Open from S3 failed — ${outcome.message} If your SSO session expired, run `aws sso login` and try again.",
            )
            is S3GetResult.Failed -> LoadResult.Failure(
                code = "s3_failed",
                message = "Open from S3 failed — ${outcome.message}",
            )
        }

        if (inNewTab && result is LoadResult.Failure) closeTab(targetTabId)
        return result
    }

    private suspend fun loadIntoActive(
        loader: SpringboardContentLoader,
        source: String,
        targetTabId: String,
    ): LoadResult {
        return try {
            val contents = loader.loadContent(source)
            val springboardConfig = SpringboardFactory.fromJson(contents, source)
            // Write directly to targetTabId — applySpringboard targets the active tab and
            // would corrupt a different tab if the user switches while the load is in flight.
            installSpringboardInTab(targetTabId, springboardConfig, tabSource = source, isDirty = false)
            val hasUnsafeActivators = hasUnsafeActivatorsForRuntime(springboardConfig)
            if (hasUnsafeActivators) {
                tabToastState(targetTabId).warning(
                    "This Springboard contains activators that execute CLI commands or process template expressions. Be sure you trust it before using."
                )
            }
            tabToastState(targetTabId).info("Springboard loaded: ${springboardConfig.name}")
            if (targetTabId == activeTabId) requestFocusAppDropdown()
            onTabsChanged()
            LoadResult.Success(targetTabId)
        } catch (e: Exception) {
            LoadResult.Failure(
                code = "load_failed",
                message = "Failed to load '$source': ${e.message ?: "unknown error"}",
            )
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

    private fun applySpringboard(
        springboardConfig: Springboard,
        source: String,
        s3AwsProfile: String? = null,
        s3LastEtag: String? = null,
    ) {
        installSpringboardInTab(
            tabId = activeTabId,
            springboardConfig = springboardConfig,
            tabSource = source,
            isDirty = false,
            s3AwsProfile = s3AwsProfile,
            s3LastEtag = s3LastEtag,
        )

        val hasUnsafeActivators = hasUnsafeActivatorsForRuntime(springboardConfig)
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

    private fun hasUnsafeActivatorsForRuntime(springboardConfig: Springboard): Boolean {
        val runtimeSpringboard = filterSpringboardForRuntime(
            springboardConfig,
            settingsManager.runtimeEnvironment,
        )
        return runtimeSpringboard.activators.any { it is UrlTemplateActivator || it is CommandActivator }
    }

    fun selectEnvironment(environmentId: String?) {
        updateActiveTab { it.copy(selectedEnvironmentId = environmentId) }
    }

    fun selectEnvironmentFromGridHeading(environmentId: String) {
        updateActiveTab {
            it.copy(
                selectedEnvironmentId = environmentId,
                selectedAppId = null,
                selectedResourceId = null,
            )
        }
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
        val currentSpringboardFilteredForRuntime = springboardFilteredForRuntime ?: return
        val activator = currentSpringboardFilteredForRuntime.indexes.activatorByCoordinate[coordinate] ?: return

        executeActivators(listOf(activator), isSingleSelection = true)
        if (settingsManager.resolveValue(ResetKeyNavAfterKeyNavActivationSetting)) {
            resetKeyNavSelections()
        }
    }

    /** Activated via grid-nav (cell click). */
    fun activateCell(coordinate: Coordinate) {
        val currentSpringboardFilteredForRuntime = springboardFilteredForRuntime ?: return
        val activator = currentSpringboardFilteredForRuntime.indexes.activatorByCoordinate[coordinate] ?: return
        executeActivators(listOf(activator), isSingleSelection = true)
        if (settingsManager.resolveValue(ResetKeyNavAfterGridNavActivationSetting)) {
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
        val currentSpringboardFilteredForRuntime = springboardFilteredForRuntime ?: return
        val activators = buildList {
            currentSpringboardFilteredForRuntime.resources.forEach { resource ->
                val activator = findActivatorWithAllEnvsFallback(environmentId, appId, resource.id)
                if (activator != null) {
                    add(activator)
                }
            }
        }
        executeActivators(activators, isSingleSelection = false)
        if (settingsManager.resolveValue(ResetKeyNavAfterGridNavActivationSetting)) {
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
        val currentSpringboardFilteredForRuntime = springboardFilteredForRuntime ?: return
        val activators = buildList {
            currentSpringboardFilteredForRuntime.apps.forEach { app ->
                val activator = findActivatorWithAllEnvsFallback(environmentId, app.id, resourceId)
                if (activator != null) {
                    add(activator)
                }
            }
        }
        executeActivators(activators, isSingleSelection = false)
        if (settingsManager.resolveValue(ResetKeyNavAfterGridNavActivationSetting)) {
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
        val activators = springboardFilteredForRuntime?.indexes?.activatorByCoordinate ?: return null
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
        val currentSpringboardFilteredForRuntime = springboardFilteredForRuntime ?: return
        val activators = buildList {
            multiSelectSet.forEach { coordinate ->
            val activator = currentSpringboardFilteredForRuntime.indexes.activatorByCoordinate[coordinate]
            if (activator != null) {
                    add(activator)
                }
            }
        }
        executeActivators(activators, isSingleSelection = false)
        multiSelectSet = emptySet()
        if (settingsManager.resolveValue(ResetKeyNavAfterGridNavActivationSetting)) {
            resetKeyNavSelections()
        }
    }

    fun getActivatorForCell(coordinate: Coordinate): Activator? {
        val currentSpringboardFilteredForRuntime = springboardFilteredForRuntime ?: return null
        return currentSpringboardFilteredForRuntime.indexes.activatorByCoordinate[coordinate]
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
                settingsManager.resolveValue(OpenUrlsInNewWindowSingleSetting)
            } else {
                settingsManager.resolveValue(OpenUrlsInNewWindowMultipleSetting)
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

        if (settingsManager.resolveValue(HideAppAfterActivationSetting)) {
            platformActivationService.hideApplicationViaPid()
        }
    }

    fun resetKeyNavSelections() {
        val currentSpringboardFilteredForRuntime = springboardFilteredForRuntime ?: return
        val environmentId = currentSpringboardFilteredForRuntime.environments.firstOrNull()?.id
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
        val currentSpringboardFilteredForRuntime = springboardFilteredForRuntime ?: return false
        return currentSpringboardFilteredForRuntime.indexes.activatorByCoordinate.keys.any { coordinate ->
            (environmentId == null ||
                coordinate.environmentId == environmentId ||
                coordinate.environmentId == ALL_ENVS_ENVIRONMENT_ID) &&
                (appId == null || coordinate.appId == appId) &&
                (resourceId == null || coordinate.resourceId == resourceId)
        }
    }
}
