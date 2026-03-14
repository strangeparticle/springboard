package com.strangeparticle.springboard.app.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.strangeparticle.springboard.app.domain.factory.SpringboardFactory
import com.strangeparticle.springboard.app.domain.model.*
import com.strangeparticle.springboard.app.loading.SpringboardLoader
import com.strangeparticle.springboard.app.platform.executeCommand
import com.strangeparticle.springboard.app.platform.openUrl
import com.strangeparticle.springboard.app.platform.openNewBrowserWindowIfAppropriate
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster

class SpringboardViewModel : ViewModel() {

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

    val environments by derivedStateOf { springboard?.environments ?: emptyList() }
    val apps by derivedStateOf { springboard?.apps ?: emptyList() }
    val resources by derivedStateOf { springboard?.resources ?: emptyList() }

    val isConfigLoaded by derivedStateOf { springboard != null }

    val appEnabledStates by derivedStateOf {
        val currentSpringboard = springboard ?: return@derivedStateOf emptyMap<String, Boolean>()
        val environmentId = selectedEnvironmentId ?: return@derivedStateOf emptyMap<String, Boolean>()
        currentSpringboard.apps.associate { app ->
            app.id to currentSpringboard.activators.any { it.environmentId == environmentId && it.appId == app.id }
        }
    }

    val resourceEnabledStates by derivedStateOf {
        val currentSpringboard = springboard ?: return@derivedStateOf emptyMap<String, Boolean>()
        val environmentId = selectedEnvironmentId ?: return@derivedStateOf emptyMap<String, Boolean>()
        val appId = selectedAppId ?: return@derivedStateOf emptyMap<String, Boolean>()
        currentSpringboard.resources.associate { resource ->
            resource.id to (currentSpringboard.indexes.activatableResourcesByEnvApp[environmentId to appId]?.contains(resource.id) == true)
        }
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

    private fun applySpringboard(springboardConfig: Springboard) {
        springboard = springboardConfig

        val defaultEnvironment = springboardConfig.environments.find {
            it.id.equals("all", ignoreCase = true)
        }
        selectedEnvironmentId = defaultEnvironment?.id ?: springboardConfig.environments.firstOrNull()?.id
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
    }

    fun selectEnvironment(environmentId: String) {
        selectedEnvironmentId = environmentId
        selectedAppId = null
        selectedResourceId = null
    }

    fun selectApp(appId: String?) {
        selectedAppId = appId
        val currentResourceId = selectedResourceId
        if (currentResourceId != null && appId != null) {
            val environmentId = selectedEnvironmentId ?: return
            val currentSpringboard = springboard ?: return
            val validResources = currentSpringboard.indexes.activatableResourcesByEnvApp[environmentId to appId] ?: emptySet()
            if (currentResourceId !in validResources) {
                selectedResourceId = null
            }
        } else {
            selectedResourceId = null
        }
    }

    fun selectResource(resourceId: String?) {
        selectedResourceId = resourceId
    }

    fun activateCurrentSelection() {
        val environmentId = selectedEnvironmentId ?: return
        val appId = selectedAppId ?: return
        val resourceId = selectedResourceId ?: return
        val currentSpringboard = springboard ?: return

        val coordinate = Coordinate(environmentId, appId, resourceId)
        val activator = currentSpringboard.indexes.activatorByCoordinate[coordinate] ?: return

        executeActivators(listOf(activator))
        resetAfterActivation()
    }

    fun activateCell(coordinate: Coordinate) {
        val currentSpringboard = springboard ?: return
        val activator = currentSpringboard.indexes.activatorByCoordinate[coordinate] ?: return
        executeActivators(listOf(activator))
    }

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
        executeActivators(activators)
    }

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
        executeActivators(activators)
    }

    fun toggleMultiSelect(coordinate: Coordinate) {
        multiSelectSet = if (coordinate in multiSelectSet) {
            multiSelectSet - coordinate
        } else {
            multiSelectSet + coordinate
        }
    }

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
        executeActivators(activators)
        multiSelectSet = emptySet()
    }

    fun getActivatorForCell(environmentId: String, appId: String, resourceId: String): Activator? {
        val currentSpringboard = springboard ?: return null
        return currentSpringboard.indexes.activatorByCoordinate[Coordinate(environmentId, appId, resourceId)]
    }

    private fun executeActivator(activator: Activator) {
        try {
            when (activator) {
                is UrlActivator -> openUrl(activator.url)
                is UrlTemplateActivator -> {
                    ToastBroadcaster.info("URL Template activators will be supported in Phase 2.")
                }
                is CommandActivator -> {
                    executeCommand(activator.commandTemplate)
                }
            }
        } catch (e: Exception) {
            ToastBroadcaster.error("An error has occurred: ${e.message}")
        }
    }

    private fun executeActivators(activators: List<Activator>) {
        if (activators.isEmpty()) return
        if (activators.any { it is UrlActivator }) {
            openNewBrowserWindowIfAppropriate()
        }
        activators.forEach(::executeActivator)
    }

    private fun resetAfterActivation() {
        val currentSpringboard = springboard ?: return
        val defaultEnvironment = currentSpringboard.environments.find {
            it.id.equals("all", ignoreCase = true)
        }
        selectedEnvironmentId = defaultEnvironment?.id ?: currentSpringboard.environments.firstOrNull()?.id
        selectedAppId = null
        selectedResourceId = null
    }

    companion object {
        const val VERSION = "3.0.0"
    }
}
