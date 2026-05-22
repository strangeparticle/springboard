package com.strangeparticle.springboard.app.runtime

import com.strangeparticle.springboard.app.domain.factory.buildSpringboardIndexes
import com.strangeparticle.springboard.app.domain.model.CommandActivator
import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment

fun filterSpringboardForRuntime(springboard: Springboard, runtimeEnvironment: RuntimeEnvironment): Springboard {
    if (runtimeEnvironment != RuntimeEnvironment.WASM) return springboard

    val filteredActivators = springboard.activators.filterNot { it is CommandActivator }
    if (filteredActivators.size == springboard.activators.size) return springboard

    return springboard.copy(
        activators = filteredActivators,
        indexes = buildSpringboardIndexes(filteredActivators, springboard.guidanceData),
    )
}
