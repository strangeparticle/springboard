package com.strangeparticle.springboard.app.persistence

import com.strangeparticle.springboard.app.settings.persistence.SettingsDto

class PersistenceServiceInMemoryFake : PersistenceService {
    private var storedSettings: SettingsDto? = null
    private var storedTabs: TabsDto? = null

    override fun loadSettings(): SettingsDto? = storedSettings

    override fun persistSettings(settings: SettingsDto) {
        storedSettings = settings
    }

    override fun loadTabs(): TabsDto? = storedTabs

    override fun persistTabs(tabs: TabsDto) {
        storedTabs = tabs
    }

    fun currentSettings(): SettingsDto? = storedSettings
}
