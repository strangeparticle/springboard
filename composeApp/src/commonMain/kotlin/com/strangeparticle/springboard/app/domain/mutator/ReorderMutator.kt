package com.strangeparticle.springboard.app.domain.mutator

import com.strangeparticle.springboard.app.domain.factory.buildSpringboardIndexes
import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.domain.model.Springboard

/**
 * Pure-function reorder mutators. Each takes the full new ordering of an entity
 * list and replaces the existing list. The supplied list must contain exactly
 * the same set of ids as the existing list; partial lists, duplicates, and
 * unknown ids all throw structured mutation errors.
 *
 * Per spec §4.2.
 */
internal fun reorderApps(springboard: Springboard, orderedAppIds: List<String>): Springboard {
    validateOrdering(springboard.apps.map { it.id }, orderedAppIds, "apps")
    val byId = springboard.apps.associateBy { it.id }
    return springboard.copy(apps = orderedAppIds.map { byId.getValue(it) })
}

internal fun reorderResources(springboard: Springboard, orderedResourceIds: List<String>): Springboard {
    validateOrdering(springboard.resources.map { it.id }, orderedResourceIds, "resources")
    val byId = springboard.resources.associateBy { it.id }
    return springboard.copy(resources = orderedResourceIds.map { byId.getValue(it) })
}

internal fun reorderEnvironments(springboard: Springboard, orderedEnvironmentIds: List<String>): Springboard {
    validateOrdering(springboard.environments.map { it.id }, orderedEnvironmentIds, "environments")
    val byId = springboard.environments.associateBy { it.id }
    return springboard.copy(environments = orderedEnvironmentIds.map { byId.getValue(it) })
}

internal fun reorderAppGroups(springboard: Springboard, orderedGroupIds: List<String>): Springboard {
    validateOrdering(springboard.appGroups.map { it.id }, orderedGroupIds, "appGroups")
    val byId = springboard.appGroups.associateBy { it.id }
    return springboard.copy(appGroups = orderedGroupIds.map { byId.getValue(it) })
}

/**
 * Reorder activators by their (env, app, resource) coordinates. The supplied
 * [orderedCoordinates] must be exactly the existing set of coordinates.
 */
internal fun reorderActivators(springboard: Springboard, orderedCoordinates: List<Coordinate>): Springboard {
    val existing = springboard.activators.map { Coordinate(it.environmentId, it.appId, it.resourceId) }
    validateOrderingOfAny(existing, orderedCoordinates, "activator coordinates")
    val byCoordinate = springboard.activators.associateBy { Coordinate(it.environmentId, it.appId, it.resourceId) }
    val newActivators = orderedCoordinates.map { byCoordinate.getValue(it) }
    return springboard.copy(
        activators = newActivators,
        indexes = buildSpringboardIndexes(newActivators, springboard.guidanceData),
    )
}

private fun validateOrdering(
    existing: List<String>,
    supplied: List<String>,
    label: String,
) {
    if (supplied.size != existing.size) {
        throw SpringboardMutationError(
            errorMessage = "Reorder of $label needs ${existing.size} ids, got ${supplied.size}.",
            code = "wrong_size",
        )
    }
    val suppliedSet = supplied.toSet()
    if (suppliedSet.size != supplied.size) {
        throw SpringboardMutationError(
            errorMessage = "Reorder of $label contains duplicate ids.",
            code = "duplicate_id",
        )
    }
    val unknown = suppliedSet - existing.toSet()
    if (unknown.isNotEmpty()) {
        throw SpringboardMutationError(
            errorMessage = "Reorder of $label references unknown id(s): ${unknown.joinToString(", ")}.",
            code = "unknown_id",
        )
    }
}

private fun <T> validateOrderingOfAny(
    existing: List<T>,
    supplied: List<T>,
    label: String,
) {
    if (supplied.size != existing.size) {
        throw SpringboardMutationError(
            errorMessage = "Reorder of $label needs ${existing.size} entries, got ${supplied.size}.",
            code = "wrong_size",
        )
    }
    val suppliedSet = supplied.toSet()
    if (suppliedSet.size != supplied.size) {
        throw SpringboardMutationError(
            errorMessage = "Reorder of $label contains duplicate entries.",
            code = "duplicate_id",
        )
    }
    val unknown = suppliedSet - existing.toSet()
    if (unknown.isNotEmpty()) {
        throw SpringboardMutationError(
            errorMessage = "Reorder of $label references unknown entry(ies): ${unknown.joinToString(", ")}.",
            code = "unknown_id",
        )
    }
}
