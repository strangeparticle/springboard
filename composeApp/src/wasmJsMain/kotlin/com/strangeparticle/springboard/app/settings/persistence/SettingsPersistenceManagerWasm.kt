package com.strangeparticle.springboard.app.settings.persistence

/**
 * WASM/browser persistence stub for v1.
 * Exact storage mechanism is deferred.
 */
class SettingsPersistenceManagerWasm : SettingsPersistenceManager {

    override fun loadDto(): UserSettingsDto? = null

    override fun saveDto(dto: UserSettingsDto) {
        // No-op for v1
    }
}
