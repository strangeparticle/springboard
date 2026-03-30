package com.strangeparticle.springboard.app.shared

import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.unit.settings.persistence.SettingsPersistenceManagerInMemoryFake

/**
 * Creates a [SettingsManager] suitable for unit tests with all defaults loaded.
 */
fun createSettingsManagerForTest(
    target: RuntimeEnvironment = RuntimeEnvironment.Test
): SettingsManager {
    val manager = SettingsManager(target, SettingsPersistenceManagerInMemoryFake())
    manager.loadSettingsAtStartup()
    return manager
}
