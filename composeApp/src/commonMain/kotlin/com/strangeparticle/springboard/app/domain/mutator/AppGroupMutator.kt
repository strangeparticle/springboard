package com.strangeparticle.springboard.app.domain.mutator

import com.strangeparticle.springboard.app.domain.model.AppGroup
import com.strangeparticle.springboard.app.domain.model.Springboard

/**
 * Pure-function mutators for the [AppGroup] list inside a [Springboard].
 *
 * Validation policy:
 * - **Add**: id must not already exist.
 * - **Update**: group must exist; id immutable.
 * - **Remove**: refused if any app references the group via its `appGroupId`.
 *
 * Per spec §4.2.
 */

internal fun addAppGroup(springboard: Springboard, group: AppGroup): Springboard {
    if (group.id.isBlank()) {
        throw SpringboardMutationError(errorMessage = "AppGroup id must not be blank.", code = "blank_id")
    }
    if (springboard.appGroups.any { it.id == group.id }) {
        throw SpringboardMutationError(
            errorMessage = "An appGroup with id '${group.id}' already exists in this springboard.",
            code = "duplicate_id",
        )
    }
    return springboard.copy(appGroups = springboard.appGroups + group)
}

internal fun updateAppGroup(
    springboard: Springboard,
    groupId: String,
    newDescription: String? = null,
): Springboard {
    val existing = springboard.appGroups.firstOrNull { it.id == groupId }
        ?: throw SpringboardMutationError(
            errorMessage = "No appGroup with id '$groupId' in this springboard.",
            code = "missing_target",
        )
    val updated = existing.copy(description = newDescription ?: existing.description)
    return springboard.copy(appGroups = springboard.appGroups.map { if (it.id == groupId) updated else it })
}

internal fun removeAppGroup(springboard: Springboard, groupId: String): Springboard {
    if (springboard.appGroups.none { it.id == groupId }) {
        throw SpringboardMutationError(
            errorMessage = "No appGroup with id '$groupId' in this springboard.",
            code = "missing_target",
        )
    }
    val refCount = springboard.apps.count { it.appGroupId == groupId }
    if (refCount > 0) {
        throw SpringboardMutationError(
            errorMessage = "Cannot remove appGroup '$groupId' — it is referenced by $refCount app(s). Update or remove those apps first.",
            code = "in_use",
        )
    }
    return springboard.copy(appGroups = springboard.appGroups.filterNot { it.id == groupId })
}
