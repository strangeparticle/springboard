package com.strangeparticle.springboard.app.shared

import com.strangeparticle.editio.client.provider.AiProviderRegistry
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.SettingsRegistry
import com.strangeparticle.springboard.app.settings.items.core.coreSettingsItems

/**
 * Creates a [SettingsManager] suitable for unit tests with all defaults loaded.
 * Assembles the registry from the same `coreSettingsItems()` + provider sources
 * production uses.
 */
fun createSettingsRegistryForTest(): SettingsRegistry =
    SettingsRegistry(coreSettingsItems() + AiProviderRegistry.all().flatMap { it.settingsItems() })

fun createSettingsManagerForTest(
    target: RuntimeEnvironment = RuntimeEnvironment.Test,
    envVars: Map<String, String> = emptyMap(),
    cliArgs: List<String> = emptyList(),
): SettingsManager {
    val manager = SettingsManager(target, createSettingsRegistryForTest(), PersistenceServiceInMemoryFake())
    manager.loadSettingsAtStartup(envVars, cliArgs)
    return manager
}
