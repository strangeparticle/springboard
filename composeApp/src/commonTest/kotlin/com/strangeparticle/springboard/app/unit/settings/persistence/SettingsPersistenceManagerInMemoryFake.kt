package com.strangeparticle.springboard.app.unit.settings.persistence

import com.strangeparticle.springboard.app.settings.persistence.SettingsPersistenceManager
import com.strangeparticle.springboard.app.settings.persistence.UserSettingsDto

/**
 * An in-memory persistence manager for tests.
 */
class SettingsPersistenceManagerInMemoryFake : SettingsPersistenceManager {
    private var stored: UserSettingsDto? = null

    override fun loadDto(): UserSettingsDto? = stored

    override fun saveDto(dto: UserSettingsDto) {
        stored = dto
    }

    /** Returns the currently stored DTO, for test assertions. */
    fun currentDto(): UserSettingsDto? = stored
}
