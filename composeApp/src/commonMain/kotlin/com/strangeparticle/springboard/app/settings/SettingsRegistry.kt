package com.strangeparticle.springboard.app.settings

/**
 * Registry of every known [SettingsItem]. Constructed at startup from a flat
 * list assembled by `main.kt`:
 *
 * ```kotlin
 * SettingsRegistry(coreSettingsItems() + AiProviderRegistry.all().flatMap { it.settingsItems() })
 * ```
 *
 * Item ids must be unique across the assembled list — duplicates fail at
 * construction time.
 */
class SettingsRegistry(items: List<SettingsItem<*>>) {

    private val byIdMap: Map<String, SettingsItem<*>> = items.associateBy { it.id }
    private val allItems: List<SettingsItem<*>> = items

    init {
        require(byIdMap.size == items.size) {
            val duplicates = items.groupBy { it.id }.filter { it.value.size > 1 }.keys
            "duplicate settings ids: $duplicates"
        }
    }

    fun byId(id: String): SettingsItem<*>? = byIdMap[id]

    fun requireById(id: String): SettingsItem<*> =
        byIdMap[id] ?: error("No registry entry for settings id: $id")

    fun all(): List<SettingsItem<*>> = allItems

    /**
     * Returns settings applicable to the given runtime environment.
     */
    fun forEnvironment(environment: RuntimeEnvironment): List<SettingsItem<*>> =
        allItems.filter { environment in it.applicability }

    /** Finds the entry whose effective env-var name matches [name]. */
    fun byEnvVarName(name: String): SettingsItem<*>? =
        allItems.firstOrNull { SettingsKeyNaming.envVarName(it) == name }

    /** Finds the entry whose URL/CLI param name matches [name]. */
    fun byUrlParamName(name: String): SettingsItem<*>? =
        allItems.firstOrNull { SettingsKeyNaming.urlParamName(it) == name }

    /** Finds the entry whose JSON key matches [name] — used by persistence. */
    fun byJsonKey(name: String): SettingsItem<*>? = byIdMap[name]
}
