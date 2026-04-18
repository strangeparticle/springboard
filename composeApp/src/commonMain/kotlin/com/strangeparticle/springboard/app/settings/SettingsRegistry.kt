package com.strangeparticle.springboard.app.settings

import com.strangeparticle.springboard.app.ui.brand.BrandRegistry

/**
 * The central settings registry — the single source of truth for all settings metadata.
 *
 * Defines each setting's key, type, default value, display metadata,
 * and environment applicability.
 *
 * External names (env var, CLI flag, URL param, JSON key) are derived
 * from the [SettingsKey] enum name by [SettingsKeyNaming].
 *
 * WARNING: This registry must remain in sync with [SettingsValues].
 *
 * Every setting registered here must have a corresponding typed property in
 * [SettingsValues], and vice versa. If you add a setting here, add a matching
 * property in [SettingsValues]. If you add a property to [SettingsValues],
 * add a matching registry entry here. Unit tests enforce this contract,
 * but developers should maintain the alignment intentionally.
 */
object SettingsRegistry {

    private val entries: Map<SettingsKey, SettingItem> = buildMap {
        register(
            SettingItem.General(
                key = SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION,
                type = Boolean::class,
                defaultValue = true,
                displayName = "Reset keyNav drop-downs after activation via keyNav",
                description = "When enabled, keyNav drop-downs are reset after activating a selection through keyNav.",
            )
        )
        register(
            SettingItem.General(
                key = SettingsKey.RESET_KEY_NAV_AFTER_GRID_NAV_ACTIVATION,
                type = Boolean::class,
                defaultValue = true,
                displayName = "Reset keyNav drop-downs after activation via grid-nav",
                description = "When enabled, keyNav drop-downs are reset after activating a selection through the grid.",
            )
        )
        register(
            SettingItem.General(
                key = SettingsKey.STARTUP_TABS,
                type = List::class,
                defaultValue = emptyList<String>(),
                displayName = "Startup Tabs",
                description = "Springboard files or URLs to open as tabs on launch.",
            )
        )
        register(
            SettingItem.General(
                key = SettingsKey.ACTIVE_BRAND,
                type = StringFromDropDown::class,
                defaultValue = StringFromDropDown(
                    defaultDropDownOptionId = BrandRegistry.defaultBrand.id,
                    dropDownOptions = BrandRegistry.entries.map { DropDownOption(it.id, it.displayName) },
                ),
                displayName = "Active Brand",
                description = "Choose the visual brand theme for the app.",
            )
        )
        register(
            SettingItem.Desktop(
                key = SettingsKey.OPEN_URLS_IN_NEW_WINDOW_SINGLE,
                type = Boolean::class,
                defaultValue = true,
                displayName = "Open URLs in new browser window for single selections",
                description = "When enabled, single-cell activations open URLs in a new browser window.",
                runtimeEnvironments = listOf(RuntimeEnvironment.DesktopOsx),
            )
        )
        register(
            SettingItem.Desktop(
                key = SettingsKey.OPEN_URLS_IN_NEW_WINDOW_MULTIPLE,
                type = Boolean::class,
                defaultValue = true,
                displayName = "Open URLs in new browser window for multiple selections",
                description = "When enabled, multi-cell activations open URLs in a new browser window.",
                runtimeEnvironments = listOf(RuntimeEnvironment.DesktopOsx),
            )
        )
        register(
            SettingItem.Desktop(
                key = SettingsKey.SURFACE_APPLESCRIPT_ERRORS,
                type = Boolean::class,
                defaultValue = false,
                displayName = "Surface AppleScript errors",
                description = "When enabled, AppleScript failures are shown as error toasts instead of being silently swallowed.",
                runtimeEnvironments = listOf(RuntimeEnvironment.DesktopOsx),
            )
        )
    }

    private fun MutableMap<SettingsKey, SettingItem>.register(item: SettingItem) {
        require(item.key !in this) { "Duplicate settings key: ${item.key}" }
        put(item.key, item)
    }

    /** Returns the registry entry for the given key, or null if not found. */
    fun get(key: SettingsKey): SettingItem? = entries[key]

    /** Returns the registry entry for the given key, throwing if not found. */
    fun require(key: SettingsKey): SettingItem =
        entries[key] ?: error("No registry entry for settings key: $key")

    /** Returns all registered settings. */
    fun allSettings(): List<SettingItem> = entries.values.toList()

    /**
     * Returns settings applicable to the given runtime environment.
     * General settings are always included. Desktop settings are included
     * only if the environment is in their runtime environments list.
     */
    fun settingsForEnvironment(environment: RuntimeEnvironment): List<SettingItem> =
        entries.values.filter { item ->
            when (item) {
                is SettingItem.General -> true
                is SettingItem.Desktop -> environment in item.runtimeEnvironments
            }
        }

    /** Finds a registry entry by its environment variable name. */
    fun findByEnvVarName(envVarName: String): SettingItem? =
        entries.values.find { SettingsKeyNaming.envVarName(it.key) == envVarName }

    /** Finds a registry entry by its URL param name. */
    fun findByUrlParamName(urlParamName: String): SettingItem? =
        entries.values.find { SettingsKeyNaming.urlParamName(it.key) == urlParamName }

    /** Finds a registry entry by its JSON key. */
    fun findByJsonKey(jsonKey: String): SettingItem? =
        entries.values.find { SettingsKeyNaming.jsonKey(it.key) == jsonKey }
}
