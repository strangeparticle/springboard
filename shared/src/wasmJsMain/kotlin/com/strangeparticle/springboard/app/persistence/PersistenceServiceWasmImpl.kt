package com.strangeparticle.springboard.app.persistence

import com.strangeparticle.springboard.app.settings.persistence.SettingsSerializer
import com.strangeparticle.springboard.app.settings.persistence.SettingsDto

@JsFun("(key) => window.localStorage.getItem(key)")
private external fun localStorageGetItem(key: String): String?

@JsFun("(key, value) => window.localStorage.setItem(key, value)")
private external fun localStorageSetItem(key: String, value: String)

/**
 * WASM persistence implementation backed by browser `localStorage`.
 *
 * Error behavior:
 * - Load methods return `null` when keys are missing or mapped values are blank.
 * - Browser storage/runtime failures are not handled locally and will propagate
 *   to callers for user-facing error handling.
 * - Settings/tabs deserialization is delegated to serializers. Invalid JSON throws
 *   and propagates to callers so parse failures can also surface as toasts.
 */

class PersistenceServiceWasmImpl : PersistenceService {
    override fun loadSettings(): SettingsDto? {
        val jsonString = localStorageGetItem(SETTINGS_PERSISTENCE_WASM_KEY) ?: return null
        if (jsonString.isBlank()) return null
        return SettingsSerializer.fromJson(jsonString)
    }

    override fun persistSettings(settings: SettingsDto) {
        val jsonString = SettingsSerializer.toJson(settings)
        localStorageSetItem(SETTINGS_PERSISTENCE_WASM_KEY, jsonString)
    }

    override fun loadTabs(): TabsDto? {
        val jsonString = localStorageGetItem(TABS_PERSISTENCE_WASM_KEY) ?: return null
        if (jsonString.isBlank()) return null
        return TabsSerializer.fromJson(jsonString)
    }

    override fun persistTabs(tabs: TabsDto) {
        val jsonString = TabsSerializer.toJson(tabs)
        localStorageSetItem(TABS_PERSISTENCE_WASM_KEY, jsonString)
    }
}
