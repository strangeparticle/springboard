package com.strangeparticle.springboard.app.domain.mutator

import com.strangeparticle.springboard.app.domain.model.Resource
import com.strangeparticle.springboard.app.domain.model.Springboard

/**
 * Pure-function mutators for the [Resource] list inside a [Springboard].
 * Same validation policy shape as [addApp] / [updateApp] / [removeApp]:
 *
 * - **Add**: id must not already exist.
 * - **Update**: resource must exist; id immutable.
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
    resourceId: String,
    newName: String? = null,
): Springboard {
    val existing = springboard.resources.firstOrNull { it.id == resourceId }
        ?: throw SpringboardMutationError(
            errorMessage = "No resource with id '$resourceId' in this springboard.",
            code = "missing_target",
        )
    val updated = existing.copy(name = newName ?: existing.name)
    return springboard.copy(resources = springboard.resources.map { if (it.id == resourceId) updated else it })
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
