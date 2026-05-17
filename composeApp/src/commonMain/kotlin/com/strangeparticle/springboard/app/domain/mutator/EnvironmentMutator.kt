package com.strangeparticle.springboard.app.domain.mutator

import com.strangeparticle.springboard.app.domain.model.ALL_ENVS_ENVIRONMENT_ID
import com.strangeparticle.springboard.app.domain.model.Environment
import com.strangeparticle.springboard.app.domain.model.Springboard

/**
 * Pure-function mutators for the [Environment] list inside a [Springboard].
 *
 * Validation policy:
 * - **Add**: id must not already exist; id must NOT equal the reserved
 *   [ALL_ENVS_ENVIRONMENT_ID] (case-insensitive — matches `SpringboardFactory`'s
 *   reserved-id check).
 * - **Update**: environment must exist; id immutable.
 * - **Remove**: refused if any activator or guidance entry references the env id.
 *
 * Per spec §4.2.
 */

internal fun addEnvironment(springboard: Springboard, environment: Environment): Springboard {
    if (environment.id.isBlank()) {
        throw SpringboardMutationError(errorMessage = "Environment id must not be blank.", code = "blank_id")
    }
    if (environment.id.equals(ALL_ENVS_ENVIRONMENT_ID, ignoreCase = true)) {
        throw SpringboardMutationError(
            errorMessage = "Environment id '${environment.id}' is reserved (case-insensitive match for the all-envs sentinel).",
            code = "reserved_id",
        )
    }
    if (springboard.environments.any { it.id == environment.id }) {
        throw SpringboardMutationError(
            errorMessage = "An environment with id '${environment.id}' already exists in this springboard.",
            code = "duplicate_id",
        )
    }
    return springboard.copy(environments = springboard.environments + environment)
}

internal fun updateEnvironment(
    springboard: Springboard,
    environmentId: String,
    newName: String? = null,
): Springboard {
    val existing = springboard.environments.firstOrNull { it.id == environmentId }
        ?: throw SpringboardMutationError(
            errorMessage = "No environment with id '$environmentId' in this springboard.",
            code = "missing_target",
        )
    val updated = existing.copy(name = newName ?: existing.name)
    return springboard.copy(
        environments = springboard.environments.map { if (it.id == environmentId) updated else it },
    )
}

internal fun removeEnvironment(springboard: Springboard, environmentId: String): Springboard {
    if (springboard.environments.none { it.id == environmentId }) {
        throw SpringboardMutationError(
            errorMessage = "No environment with id '$environmentId' in this springboard.",
            code = "missing_target",
        )
    }
    val activatorRefCount = springboard.activators.count { it.environmentId == environmentId }
    if (activatorRefCount > 0) {
        throw SpringboardMutationError(
            errorMessage = "Cannot remove environment '$environmentId' — it is referenced by $activatorRefCount activator(s). Remove those first.",
            code = "in_use",
        )
    }
    val guidanceRefCount = springboard.guidanceData.count { it.environmentId == environmentId }
    if (guidanceRefCount > 0) {
        throw SpringboardMutationError(
            errorMessage = "Cannot remove environment '$environmentId' — it is referenced by $guidanceRefCount guidance entry(ies). Remove those first.",
            code = "in_use",
        )
    }
    return springboard.copy(environments = springboard.environments.filterNot { it.id == environmentId })
}
