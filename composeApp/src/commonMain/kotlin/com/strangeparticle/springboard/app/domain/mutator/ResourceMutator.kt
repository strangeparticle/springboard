package com.strangeparticle.springboard.app.domain.mutator

import com.strangeparticle.springboard.app.domain.factory.buildSpringboardIndexes
import com.strangeparticle.springboard.app.domain.model.Resource
import com.strangeparticle.springboard.app.domain.model.Springboard

/**
 * Pure-function mutators for the [Resource] list inside a [Springboard].
 * Same validation policy shape as [addApp] / [updateApp] / [removeApp]:
 *
 * - **Add**: id must not already exist.
 * - **Update**: replacement resource must keep the same id as an existing resource.
 * - **Change name**: resource must exist; id stays unchanged.
 * - **Change id**: resource must exist; id changes rewrite resource references.
 * - **Remove**: refused if any activator or guidance entry references the resource.
 *
 * Per spec §4.2.
 */

internal fun addResource(springboard: Springboard, resource: Resource): Springboard {
    if (resource.id.isBlank()) {
        throw SpringboardMutationError(errorMessage = "Resource id must not be blank.", code = "blank_id")
    }
    if (springboard.resources.any { it.id == resource.id }) {
        throw SpringboardMutationError(
            errorMessage = "A resource with id '${resource.id}' already exists in this springboard.",
            code = "duplicate_id",
        )
    }
    return springboard.copy(resources = springboard.resources + resource)
}

internal fun updateResource(
    springboard: Springboard,
    updatedResource: Resource,
): Springboard {
    if (updatedResource.id.isBlank()) {
        throw SpringboardMutationError(errorMessage = "Resource id must not be blank.", code = "blank_id")
    }
    if (springboard.resources.none { it.id == updatedResource.id }) {
        throw SpringboardMutationError(
            errorMessage = "No resource with id '${updatedResource.id}' in this springboard.",
            code = "missing_target",
        )
    }
    return springboard.withResourceUpdated(updatedResource.id, updatedResource)
}

internal fun changeResourceName(
    springboard: Springboard,
    resourceId: String,
    newName: String,
): Springboard {
    val existing = springboard.resources.firstOrNull { it.id == resourceId }
        ?: throw SpringboardMutationError(
            errorMessage = "No resource with id '$resourceId' in this springboard.",
            code = "missing_target",
        )
    return updateResource(springboard, existing.copy(name = newName))
}

internal fun changeResourceId(
    springboard: Springboard,
    resourceId: String,
    newId: String,
): Springboard {
    val existing = springboard.resources.firstOrNull { it.id == resourceId }
        ?: throw SpringboardMutationError(
            errorMessage = "No resource with id '$resourceId' in this springboard.",
            code = "missing_target",
        )
    if (newId.isBlank()) {
        throw SpringboardMutationError(errorMessage = "Resource id must not be blank.", code = "blank_id")
    }
    if (newId != resourceId && springboard.resources.any { it.id == newId }) {
        throw SpringboardMutationError(
            errorMessage = "A resource with id '$newId' already exists in this springboard.",
            code = "duplicate_id",
        )
    }

    return if (newId == resourceId) {
        springboard
    } else {
        springboard.withResourceIdReplacedThroughoutSpringboardTree(
            oldResourceId = resourceId,
            updatedResource = existing.copy(id = newId),
        )
    }
}

private fun Springboard.withResourceUpdated(
    resourceId: String,
    updatedResource: Resource,
): Springboard = copy(resources = resources.map { if (it.id == resourceId) updatedResource else it })

/**
 * A resource id is stored both on the resource itself and anywhere an app /
 * environment / resource coordinate refers to it. Changing the id must update
 * the resource entry and every coordinate reference in the Springboard model.
 */
private fun Springboard.withResourceIdReplacedThroughoutSpringboardTree(
    oldResourceId: String,
    updatedResource: Resource,
): Springboard {
    val newActivators = activators.map { if (it.resourceId == oldResourceId) it.withResourceId(updatedResource.id) else it }
    val newGuidanceData = guidanceData.map { if (it.resourceId == oldResourceId) it.copy(resourceId = updatedResource.id) else it }
    return copy(
        resources = resources.map { if (it.id == oldResourceId) updatedResource else it },
        activators = newActivators,
        guidanceData = newGuidanceData,
        indexes = buildSpringboardIndexes(newActivators, newGuidanceData),
    )
}

internal fun removeResource(springboard: Springboard, resourceId: String): Springboard {
    if (springboard.resources.none { it.id == resourceId }) {
        throw SpringboardMutationError(
            errorMessage = "No resource with id '$resourceId' in this springboard.",
            code = "missing_target",
        )
    }
    val activatorRefCount = springboard.activators.count { it.resourceId == resourceId }
    if (activatorRefCount > 0) {
        throw SpringboardMutationError(
            errorMessage = "Cannot remove resource '$resourceId' — it is referenced by $activatorRefCount activator(s). Remove those first.",
            code = "in_use",
        )
    }
    val guidanceRefCount = springboard.guidanceData.count { it.resourceId == resourceId }
    if (guidanceRefCount > 0) {
        throw SpringboardMutationError(
            errorMessage = "Cannot remove resource '$resourceId' — it is referenced by $guidanceRefCount guidance entry(ies). Remove those first.",
            code = "in_use",
        )
    }
    return springboard.copy(resources = springboard.resources.filterNot { it.id == resourceId })
}
