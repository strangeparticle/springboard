package com.strangeparticle.springboard.app.settings.persistence

import java.io.File

/**
 * Desktop persistence adapter that reads and writes user settings
 * to ~/.springboard/springboard_conf.json.
 *
 * Changes made in the app are written immediately.
 * The file is read at startup as the user settings source.
 *
 * Direct user editing of the file is fine but not fully supported
 * - we don't attempt to auto-reload "live" changes to that file
 */
class SettingsPersistenceManagerDesktop : SettingsPersistenceManager {

    private val configDir: File by lazy {
        File(System.getProperty("user.home"), ".springboard")
    }

    private val configFile: File by lazy {
        File(configDir, SETTINGS_CONFIG_FILENAME)
    }

    override fun loadDto(): UserSettingsDto? {
        if (!configFile.exists()) return null
        return try {
            val jsonString = configFile.readText()
            if (jsonString.isBlank()) null
            else SettingsSerializer.fromJson(jsonString)
        } catch (e: Exception) {
            println("[Springboard] Failed to read settings file: ${e.message}")
            null
        }
    }

    override fun saveDto(dto: UserSettingsDto) {
        try {
            if (!configDir.exists()) {
                configDir.mkdirs()
            }
            val jsonString = SettingsSerializer.toJson(dto)
            configFile.writeText(jsonString)
        } catch (e: Exception) {
            println("[Springboard] Failed to write settings file: ${e.message}")
        }
    }
}
