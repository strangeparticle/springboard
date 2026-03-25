package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.persistence.SettingsPersistenceManager
import com.strangeparticle.springboard.app.settings.persistence.UserSettingsDto

/**
 * An in-memory persistence manager for tests.
 */
class InMemorySettingsPersistenceManager : SettingsPersistenceManager {
    private var stored: UserSettingsDto? = null

    override fun loadDto(): UserSettingsDto? = stored

    override fun saveDto(dto: UserSettingsDto) {
        stored = dto
    }

    /** Returns the currently stored DTO, for test assertions. */
    fun currentDto(): UserSettingsDto? = stored
}

/**
 * Creates a [SettingsManager] suitable for unit tests with all defaults loaded.
 */
fun createTestSettingsManager(
    target: RuntimeEnvironment = RuntimeEnvironment.DesktopOsx
): SettingsManager {
    val manager = SettingsManager(target, InMemorySettingsPersistenceManager())
    manager.loadSettingsAtStartup()
    return manager
}
