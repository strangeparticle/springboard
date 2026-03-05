package com.strangeparticle.springboard.app.domain.model

data class SpringboardIndexes(
    val activatorByCoordinate: Map<Coordinate, Activator>,
    val activatableResourcesByApp: Map<String, Set<String>>,
    val activatableAppsByResource: Map<String, Set<String>>,
    val activatableResourcesByEnvApp: Map<Pair<String, String>, Set<String>>
)
