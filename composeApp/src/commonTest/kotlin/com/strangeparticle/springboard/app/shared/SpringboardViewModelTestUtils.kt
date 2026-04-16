package com.strangeparticle.springboard.app.shared

import com.strangeparticle.springboard.app.persistence.PersistenceService
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel

fun createViewModelForTest(
    target: RuntimeEnvironment = RuntimeEnvironment.Test,
    persistenceService: PersistenceService = PersistenceServiceInMemoryFake(),
): SpringboardViewModel {
    val settingsManager = SettingsManager(target, persistenceService).also { it.loadSettingsAtStartup() }
    return SpringboardViewModel(settingsManager, persistenceService)
}
