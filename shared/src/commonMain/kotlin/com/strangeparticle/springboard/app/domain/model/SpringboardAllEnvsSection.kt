package com.strangeparticle.springboard.app.domain.model

/**
 * Resources that have at least one ALL-envs activator, in the same declaration
 * order as `springboard.resources`. The all-envs section in the grid renders
 * only these resources to keep the section visually compact.
 */
fun Springboard.allEnvsResources(): List<Resource> {
    val allEnvsResourceIds = activators
        .filter { it.environmentId == ALL_ENVS_ENVIRONMENT_ID }
        .map { it.resourceId }
        .toSet()
    return resources.filter { it.id in allEnvsResourceIds }
}

/**
 * True when the springboard contains any ALL-envs activator. Used to decide
 * whether to render the all-envs section, its heading, and the spacer row
 * between the all-envs and env-specific sections.
 */
fun Springboard.hasAnyAllEnvsActivators(): Boolean =
    activators.any { it.environmentId == ALL_ENVS_ENVIRONMENT_ID }

/**
 * Resources that have at least one env-specific activator (i.e. an activator
 * whose environmentId is not the reserved ALL id), in declaration order. The
 * env section in the grid renders only these resources so resources whose only
 * activator is an ALL-envs activator do not duplicate into the env section.
 */
fun Springboard.envSpecificResources(): List<Resource> {
    val envSpecificResourceIds = activators
        .filter { it.environmentId != ALL_ENVS_ENVIRONMENT_ID }
        .map { it.resourceId }
        .toSet()
    return resources.filter { it.id in envSpecificResourceIds }
}

/**
 * User-facing display name for an environment id. Configured environments
 * resolve to their `Environment.name`; the reserved `"ALL"` id displays as
 * `"All Environments"`. Falls back to the raw id when nothing matches.
 */
fun Springboard.displayNameForEnvironmentId(environmentId: String): String {
    if (environmentId == ALL_ENVS_ENVIRONMENT_ID) return ALL_ENVS_DISPLAY_NAME
    return environments.firstOrNull { it.id == environmentId }?.name ?: environmentId
}

const val ALL_ENVS_DISPLAY_NAME = "All Environments"
