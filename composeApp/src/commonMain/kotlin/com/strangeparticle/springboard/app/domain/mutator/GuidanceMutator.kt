package com.strangeparticle.springboard.app.domain.mutator

import com.strangeparticle.springboard.app.domain.factory.buildSpringboardIndexes
import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.domain.model.GuidanceData
import com.strangeparticle.springboard.app.domain.model.Springboard

/** Pure-function mutators for the guidance-data list. */
internal fun addGuidance(springboard: Springboard, guidance: GuidanceData): Springboard {
    validateGuidanceLines(guidance.guidanceLines)
    val coordinate = Coordinate(guidance.environmentId, guidance.appId, guidance.resourceId)
    validateCoordinateReferences(springboard, guidance.environmentId, guidance.appId, guidance.resourceId)

    if (!springboard.indexes.activatorByCoordinate.containsKey(coordinate)) {
        throw SpringboardMutationError(
            errorMessage = "Cannot add guidance at (env=${guidance.environmentId}, app=${guidance.appId}, " +
                "resource=${guidance.resourceId}) — there is no activator at that coordinate. " +
                "Add the activator first.",
            code = "missing_activator",
        )
    }
    if (springboard.guidanceData.any { existingCoordinate(it) == coordinate }) {
        throw SpringboardMutationError(
            errorMessage = "A guidance entry already exists at coordinate (env=${guidance.environmentId}, " +
                "app=${guidance.appId}, resource=${guidance.resourceId}). " +
                "Update or remove it first.",
            code = "duplicate_coordinate",
        )
    }

    val newGuidance = springboard.guidanceData + guidance
    return springboard.copy(
        guidanceData = newGuidance,
        indexes = buildSpringboardIndexes(springboard.activators, newGuidance),
    )
}

internal fun updateGuidance(
    springboard: Springboard,
    coordinate: Coordinate,
    newGuidanceLines: List<String>,
): Springboard {
    validateGuidanceLines(newGuidanceLines)
    val existing = springboard.guidanceData.firstOrNull { existingCoordinate(it) == coordinate }
        ?: throw SpringboardMutationError(
            errorMessage = "No guidance entry at coordinate (env=${coordinate.environmentId}, " +
                "app=${coordinate.appId}, resource=${coordinate.resourceId}).",
            code = "missing_target",
        )

    val newGuidance = springboard.guidanceData.map {
        if (it === existing) existing.copy(guidanceLines = newGuidanceLines) else it
    }
    return springboard.copy(
        guidanceData = newGuidance,
        indexes = buildSpringboardIndexes(springboard.activators, newGuidance),
    )
}

internal fun removeGuidance(springboard: Springboard, coordinate: Coordinate): Springboard {
    val existing = springboard.guidanceData.firstOrNull { existingCoordinate(it) == coordinate }
        ?: throw SpringboardMutationError(
            errorMessage = "No guidance entry at coordinate (env=${coordinate.environmentId}, " +
                "app=${coordinate.appId}, resource=${coordinate.resourceId}).",
            code = "missing_target",
        )

    val newGuidance = springboard.guidanceData.filterNot { it === existing }
    return springboard.copy(
        guidanceData = newGuidance,
        indexes = buildSpringboardIndexes(springboard.activators, newGuidance),
    )
}

private fun validateGuidanceLines(guidanceLines: List<String>) {
    if (guidanceLines.isEmpty()) {
        throw SpringboardMutationError(
            errorMessage = "guidance_lines must not be empty.",
            code = "empty_guidance_lines",
        )
    }
    if (guidanceLines.any { it.isBlank() }) {
        throw SpringboardMutationError(
            errorMessage = "guidance_lines must not contain blank lines.",
            code = "empty_guidance_line",
        )
    }
}

private fun existingCoordinate(guidance: GuidanceData): Coordinate =
    Coordinate(guidance.environmentId, guidance.appId, guidance.resourceId)
