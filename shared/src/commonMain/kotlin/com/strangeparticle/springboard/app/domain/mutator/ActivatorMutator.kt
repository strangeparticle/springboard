package com.strangeparticle.springboard.app.domain.mutator

import com.strangeparticle.springboard.app.domain.factory.buildSpringboardIndexes
import com.strangeparticle.springboard.app.domain.model.ALL_ENVS_ENVIRONMENT_ID
import com.strangeparticle.springboard.app.domain.model.Activator
import com.strangeparticle.springboard.app.domain.model.CommandActivator
import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.domain.model.TerminalActivator
import com.strangeparticle.springboard.app.domain.model.UrlActivator
import com.strangeparticle.springboard.app.domain.model.UrlTemplateActivator

/** Pure-function mutators for the activators list. */
internal fun addActivator(springboard: Springboard, activator: Activator): Springboard {
    validateCoordinateReferences(
        springboard,
        activator.environmentId,
        activator.appId,
        activator.resourceId,
    )

    val coordinate = Coordinate(activator.environmentId, activator.appId, activator.resourceId)
    if (springboard.indexes.activatorByCoordinate.containsKey(coordinate)) {
        throw SpringboardMutationError(
            errorMessage = "An activator already exists at coordinate (env=${activator.environmentId}, " +
                "app=${activator.appId}, resource=${activator.resourceId}). " +
                "Update or remove it first.",
            code = "duplicate_coordinate",
        )
    }

    val newActivators = springboard.activators + activator
    return springboard.copy(
        activators = newActivators,
        indexes = buildSpringboardIndexes(newActivators, springboard.guidanceData),
    )
}

internal fun updateActivator(
    springboard: Springboard,
    coordinate: Coordinate,
    newUrl: String? = null,
    newUrlTemplate: String? = null,
    newCommandTemplate: String? = null,
    newWorkingDirectory: String? = null,
    newCommand: String? = null,
): Springboard {
    val existing = springboard.indexes.activatorByCoordinate[coordinate]
        ?: throw SpringboardMutationError(
            errorMessage = "No activator at coordinate (env=${coordinate.environmentId}, " +
                "app=${coordinate.appId}, resource=${coordinate.resourceId}).",
            code = "missing_target",
        )

    val updated: Activator = when (existing) {
        is UrlActivator -> {
            if (newUrlTemplate != null || newCommandTemplate != null || newWorkingDirectory != null || newCommand != null) {
                throw SpringboardMutationError(
                    errorMessage = "URL activator only accepts a `url` payload — to change type, remove + add.",
                    code = "wrong_field_for_type",
                )
            }
            existing.copy(url = newUrl ?: existing.url)
        }
        is UrlTemplateActivator -> {
            if (newUrl != null || newCommandTemplate != null || newWorkingDirectory != null || newCommand != null) {
                throw SpringboardMutationError(
                    errorMessage = "URL-template activator only accepts a `url_template` payload — to change type, remove + add.",
                    code = "wrong_field_for_type",
                )
            }
            existing.copy(urlTemplate = newUrlTemplate ?: existing.urlTemplate)
        }
        is CommandActivator -> {
            if (newUrl != null || newUrlTemplate != null || newWorkingDirectory != null || newCommand != null) {
                throw SpringboardMutationError(
                    errorMessage = "Command activator only accepts a `command_template` payload — to change type, remove + add.",
                    code = "wrong_field_for_type",
                )
            }
            existing.copy(commandTemplate = newCommandTemplate ?: existing.commandTemplate)
        }
        is TerminalActivator -> {
            if (newUrl != null || newUrlTemplate != null || newCommandTemplate != null) {
                throw SpringboardMutationError(
                    errorMessage = "Terminal activator only accepts `working_directory` / `command` payloads — to change type, remove + add.",
                    code = "wrong_field_for_type",
                )
            }
            existing.copy(
                workingDirectory = newWorkingDirectory ?: existing.workingDirectory,
                command = newCommand ?: existing.command,
            )
        }
    }

    val newActivators = springboard.activators.map { if (it === existing) updated else it }
    return springboard.copy(
        activators = newActivators,
        indexes = buildSpringboardIndexes(newActivators, springboard.guidanceData),
    )
}

internal fun removeActivator(springboard: Springboard, coordinate: Coordinate): Springboard {
    val existing = springboard.indexes.activatorByCoordinate[coordinate]
        ?: throw SpringboardMutationError(
            errorMessage = "No activator at coordinate (env=${coordinate.environmentId}, " +
                "app=${coordinate.appId}, resource=${coordinate.resourceId}).",
            code = "missing_target",
        )

    val guidanceRefCount = springboard.guidanceData.count {
        Coordinate(it.environmentId, it.appId, it.resourceId) == coordinate
    }
    if (guidanceRefCount > 0) {
        throw SpringboardMutationError(
            errorMessage = "Cannot remove activator at this coordinate — guidance entry exists for the same " +
                "coordinate. Remove the guidance entry first.",
            code = "in_use",
        )
    }

    val newActivators = springboard.activators.filterNot { it === existing }
    return springboard.copy(
        activators = newActivators,
        indexes = buildSpringboardIndexes(newActivators, springboard.guidanceData),
    )
}

internal fun validateCoordinateReferences(
    springboard: Springboard,
    environmentId: String,
    appId: String,
    resourceId: String,
) {
    val envExistsOrAll = environmentId == ALL_ENVS_ENVIRONMENT_ID ||
        springboard.environments.any { it.id == environmentId }
    if (!envExistsOrAll) {
        throw SpringboardMutationError(
            errorMessage = "Environment '$environmentId' does not exist in this springboard " +
                "(use '$ALL_ENVS_ENVIRONMENT_ID' for the all-envs sentinel).",
            code = "missing_reference",
        )
    }
    if (springboard.apps.none { it.id == appId }) {
        throw SpringboardMutationError(
            errorMessage = "App '$appId' does not exist in this springboard.",
            code = "missing_reference",
        )
    }
    if (springboard.resources.none { it.id == resourceId }) {
        throw SpringboardMutationError(
            errorMessage = "Resource '$resourceId' does not exist in this springboard.",
            code = "missing_reference",
        )
    }
}
