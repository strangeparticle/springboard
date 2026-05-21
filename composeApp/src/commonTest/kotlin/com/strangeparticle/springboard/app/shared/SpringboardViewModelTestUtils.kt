package com.strangeparticle.springboard.app.shared

import com.strangeparticle.editio.client.provider.AiProviderRegistry
import com.strangeparticle.springboard.app.persistence.PersistenceService
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.SettingsRegistry
import com.strangeparticle.springboard.app.settings.items.core.coreSettingsItems
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel

fun createViewModelForTest(
    target: RuntimeEnvironment = RuntimeEnvironment.Test,
    persistenceService: PersistenceService = PersistenceServiceInMemoryFake(),
): SpringboardViewModel {
    val registry = SettingsRegistry(
        coreSettingsItems() + AiProviderRegistry.all().flatMap { it.settingsItems() }
    )
    val settingsManager = SettingsManager(target, registry, persistenceService).also { it.loadSettingsAtStartup() }
    return SpringboardViewModel(settingsManager, persistenceService)
}
