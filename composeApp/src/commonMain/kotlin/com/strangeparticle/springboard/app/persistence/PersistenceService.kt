package com.strangeparticle.springboard.app.persistence

import com.strangeparticle.springboard.app.settings.persistence.SettingsDto

const val SETTINGS_PERSISTENCE_FILENAME = "springboard_settings.json"
const val SETTINGS_PERSISTENCE_WASM_KEY = "springboard_settings"

const val TABS_PERSISTENCE_FILENAME = "springboard_tabs.json"
const val TABS_PERSISTENCE_WASM_KEY = "springboard_tabs"

interface PersistenceService {
    fun loadSettings(): SettingsDto?
    fun persistSettings(settings: SettingsDto)

    fun loadTabs(): TabsDto?
    fun persistTabs(tabs: TabsDto)
}
