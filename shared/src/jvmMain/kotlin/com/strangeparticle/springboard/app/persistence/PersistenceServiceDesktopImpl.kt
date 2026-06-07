package com.strangeparticle.springboard.app.persistence

import com.strangeparticle.springboard.app.settings.persistence.SettingsSerializer
import com.strangeparticle.springboard.app.settings.persistence.SettingsDto
import java.io.File

/**
 * Desktop persistence implementation backed by files under `~/.springboard`.
 *
 * Error behavior:
 * - Load methods return `null` only when the target file is absent or empty.
 * - Filesystem errors (for example permissions/read/write failures) are not swallowed;
 *   they propagate to callers so the app can surface an error toast.
 * - Settings/tabs deserialization is delegated to serializers. Invalid JSON throws
 *   and propagates to callers so parse failures can also surface as toasts.
 */
class PersistenceServiceDesktopImpl(
    private val homeDirectoryPath: String = System.getProperty("user.home"),
) : PersistenceService {

    private val configDir: File by lazy {
        File(homeDirectoryPath, ".springboard")
    }

    private val settingsFile: File by lazy {
        File(configDir, SETTINGS_PERSISTENCE_FILENAME)
    }

    private val tabsFile: File by lazy {
        File(configDir, TABS_PERSISTENCE_FILENAME)
    }

    override fun loadSettings(): SettingsDto? {
        if (!settingsFile.exists()) return null
        val jsonString = settingsFile.readText()
        if (jsonString.isBlank()) return null
        return SettingsSerializer.fromJson(jsonString)
    }

    override fun persistSettings(settings: SettingsDto) {
        ensureConfigDirectoryExists()
        val jsonString = SettingsSerializer.toJson(settings)
        settingsFile.writeText(jsonString)
    }

    override fun loadTabs(): TabsDto? {
        if (!tabsFile.exists()) return null
        val jsonString = tabsFile.readText()
        if (jsonString.isBlank()) return null
        return TabsSerializer.fromJson(jsonString)
    }

    override fun persistTabs(tabs: TabsDto) {
        ensureConfigDirectoryExists()
        val jsonString = TabsSerializer.toJson(tabs)
        tabsFile.writeText(jsonString)
    }

    private fun ensureConfigDirectoryExists() {
        if (!configDir.exists() && !configDir.mkdirs()) {
            throw IllegalStateException("Failed to create persistence directory: ${configDir.absolutePath}")
        }
    }
}
