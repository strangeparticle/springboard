package com.strangeparticle.springboard.app.settings.persistence

/** The filename used for persisted user settings. */
const val SETTINGS_CONFIG_FILENAME = "springboard_conf.json"

/**
 * Manages persistence of user settings values.
 *
 * The persistence system is separated from both UI code and settings-resolution logic.
 * Shared code defines the data model and serialization format.
 * Platform-specific code connects this manager to the appropriate storage backend.
 *
 * Only the user-settings layer is persisted. Other source layers (defaults,
 * env vars, CLI) do not need persistence.
 */
interface SettingsPersistenceManager {

    /**
     * Loads persisted user settings as a [UserSettingsDto].
     * Returns null if no persisted settings exist or if loading fails.
     */
    fun loadDto(): UserSettingsDto?

    /**
     * Saves user settings to the storage backend.
     */
    fun saveDto(dto: UserSettingsDto)
}
