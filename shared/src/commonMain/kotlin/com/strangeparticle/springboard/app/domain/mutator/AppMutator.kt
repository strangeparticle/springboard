package com.strangeparticle.springboard.app.domain.mutator

import com.strangeparticle.springboard.app.domain.factory.buildSpringboardIndexes
import com.strangeparticle.springboard.app.domain.model.App
import com.strangeparticle.springboard.app.domain.model.Springboard

/**
 * Pure-function mutators for the [App] list inside a [Springboard]. Each function
 * takes the current springboard and returns the updated [Springboard]. No viewmodel,
 * no UI, no IO.
 *
 * Validation policy:
 * - **Add**: id must not already exist; `appGroupId` (when non-null) must exist.
 * - **Update**: app must exist; `appGroupId` (when non-null) must exist. Id is
 *   immutable on update — to "rename", the model removes and re-adds.
 * - **Remove**: refuses if any activator or guidance entry references the app
 *   (cascade-by-refusal). The model can clean up activators / guidance first.
 *
 * Per spec §4.2.
 */

internal fun addApp(springboard: Springboard, app: App): Springboard {
    if (app.id.isBlank()) {
        throw SpringboardMutationError(errorMessage = "App id must not be blank.", code = "blank_id")
    }
    if (springboard.apps.any { it.id == app.id }) {
        throw SpringboardMutationError(
            errorMessage = "An app with id '${app.id}' already exists in this springboard.",
            code = "duplicate_id",
        )
    }
    app.appGroupId?.let { groupId ->
        if (springboard.appGroups.none { it.id == groupId }) {
            throw SpringboardMutationError(
                errorMessage = "App '${app.id}' references appGroup '$groupId' which does not exist.",
                code = "missing_reference",
            )
        }
    }
    return springboard.copy(apps = springboard.apps + app)
}

internal fun updateApp(
    springboard: Springboard,
    appId: String,
    newName: String? = null,
    newAppGroupId: String? = null,
    clearAppGroupId: Boolean = false,
): Springboard {
    if (newAppGroupId != null && clearAppGroupId) {
        throw SpringboardMutationError(
            errorMessage = "Cannot set and clear app_group_id in the same request.",
            code = "conflicting_fields",
        )
    }
    val existing = springboard.apps.firstOrNull { it.id == appId }
        ?: throw SpringboardMutationError(
            errorMessage = "No app with id '$appId' in this springboard.",
            code = "missing_target",
        )
    if (newAppGroupId != null && springboard.appGroups.none { it.id == newAppGroupId }) {
        throw SpringboardMutationError(
            errorMessage = "App '$appId' references appGroup '$newAppGroupId' which does not exist.",
            code = "missing_reference",
        )
    }
    val updated = existing.copy(
        name = newName ?: existing.name,
        appGroupId = when {
            clearAppGroupId -> null
            newAppGroupId != null -> newAppGroupId
            else -> existing.appGroupId
        },
    )
    return springboard.copy(apps = springboard.apps.map { if (it.id == appId) updated else it })
}

internal fun changeAppId(
    springboard: Springboard,
    appId: String,
    newId: String,
): Springboard {
    val existing = springboard.apps.firstOrNull { it.id == appId }
        ?: throw SpringboardMutationError(
            errorMessage = "No app with id '$appId' in this springboard.",
            code = "missing_target",
        )
    if (newId.isBlank()) {
        throw SpringboardMutationError(errorMessage = "App id must not be blank.", code = "blank_id")
    }
    if (newId != appId && springboard.apps.any { it.id == newId }) {
        throw SpringboardMutationError(
            errorMessage = "An app with id '$newId' already exists in this springboard.",
            code = "duplicate_id",
        )
    }

    if (newId == appId) return springboard

    val newActivators = springboard.activators.map { if (it.appId == appId) it.withAppId(newId) else it }
    val newGuidanceData = springboard.guidanceData.map { if (it.appId == appId) it.copy(appId = newId) else it }
    return springboard.copy(
        apps = springboard.apps.map { if (it.id == appId) existing.copy(id = newId) else it },
        activators = newActivators,
        guidanceData = newGuidanceData,
        indexes = buildSpringboardIndexes(newActivators, newGuidanceData),
    )
}

internal fun removeApp(springboard: Springboard, appId: String): Springboard {
    if (springboard.apps.none { it.id == appId }) {
        throw SpringboardMutationError(
            errorMessage = "No app with id '$appId' in this springboard.",
            code = "missing_target",
        )
    }
    val activatorRefCount = springboard.activators.count { it.appId == appId }
    if (activatorRefCount > 0) {
        throw SpringboardMutationError(
            errorMessage = "Cannot remove app '$appId' — it is referenced by $activatorRefCount activator(s). Remove those first.",
            code = "in_use",
        )
    }
    val guidanceRefCount = springboard.guidanceData.count { it.appId == appId }
    if (guidanceRefCount > 0) {
        throw SpringboardMutationError(
            errorMessage = "Cannot remove app '$appId' — it is referenced by $guidanceRefCount guidance entry(ies). Remove those first.",
            code = "in_use",
        )
    }
    return springboard.copy(apps = springboard.apps.filterNot { it.id == appId })
}
