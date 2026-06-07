package com.strangeparticle.springboard.app.domain.factory

import com.strangeparticle.springboard.app.domain.model.Activator
import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.domain.model.GuidanceData
import com.strangeparticle.springboard.app.domain.model.SpringboardIndexes

/**
 * Pure helper that builds [SpringboardIndexes] from a list of activators and
 * guidance entries. Used by:
 *
 * - [SpringboardFactory.fromJson] when loading a springboard from disk.
 * - The AI editing mutators (`activator*`, `guidance*`, `reorder*`) when a
 *   mutation has changed the activator or guidance lists and the indexes must
 *   be rebuilt to match.
 *
 * Indexes are pure functions of the activator and guidance lists, so we don't
 * mutate them in place — callers swap in the rebuilt [SpringboardIndexes] after
 * calling this.
 */
internal fun buildSpringboardIndexes(
    activators: List<Activator>,
    guidanceData: List<GuidanceData>,
): SpringboardIndexes {
    val byCoordinate = mutableMapOf<Coordinate, Activator>()
    val resourcesByApp = mutableMapOf<String, MutableSet<String>>()
    val appsByResource = mutableMapOf<String, MutableSet<String>>()
    val resourcesByEnvApp = mutableMapOf<Pair<String, String>, MutableSet<String>>()

    for (activator in activators) {
        val coordinate = Coordinate(activator.environmentId, activator.appId, activator.resourceId)
        byCoordinate[coordinate] = activator

        resourcesByApp.getOrPut(activator.appId) { mutableSetOf() }.add(activator.resourceId)
        appsByResource.getOrPut(activator.resourceId) { mutableSetOf() }.add(activator.appId)

        val envAppKey = activator.environmentId to activator.appId
        resourcesByEnvApp.getOrPut(envAppKey) { mutableSetOf() }.add(activator.resourceId)
    }

    val guidanceByCoordinate = guidanceData.associateBy {
        Coordinate(it.environmentId, it.appId, it.resourceId)
    }

    return SpringboardIndexes(
        activatorByCoordinate = byCoordinate,
        activatableResourcesByApp = resourcesByApp,
        activatableAppsByResource = appsByResource,
        activatableResourcesByEnvApp = resourcesByEnvApp,
        guidanceByCoordinate = guidanceByCoordinate,
    )
}
