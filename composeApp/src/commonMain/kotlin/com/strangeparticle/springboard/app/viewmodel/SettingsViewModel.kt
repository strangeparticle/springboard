package com.strangeparticle.springboard.app.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.strangeparticle.springboard.app.settings.PRECEDENCE_CHAIN
import com.strangeparticle.springboard.app.settings.RuntimeEnvironment
import com.strangeparticle.springboard.app.settings.SettingsItem
import com.strangeparticle.springboard.app.settings.SettingsItemContext
import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.settings.SettingsSource
import com.strangeparticle.springboard.app.settings.items.core.ActiveBrandSetting
import com.strangeparticle.springboard.app.settings.items.core.StartupTabsSetting
import io.ktor.client.HttpClient

/**
 * Manages Settings screen state and brokers reads / writes through [SettingsManager].
 * Also supplies the [SettingsItemContext] for items that need cross-cutting access
 * (HTTP, sibling-setting reads — today: `DropDownFromApiCallSettingsItem.loadOptions`
 * and `AiProvider.createClient`).
 */
class SettingsViewModel(
    private val settingsManager: SettingsManager,
    private val httpClient: HttpClient,
) : ViewModel() {

    val runtimeEnvironment: RuntimeEnvironment = settingsManager.runtimeEnvironment

    val registry get() = settingsManager.registry

    /**
     * Counter that increments on every user setting change. UI reads it so that
     * settings-dependent composables recompose.
     */
    var settingsVersion by mutableStateOf(0)
        private set

    private val context: SettingsItemContext = object : SettingsItemContext {
        override val httpClient: HttpClient get() = this@SettingsViewModel.httpClient
        override fun <T : Any> get(item: SettingsItem<T>): T? = getResolvedValueOrNull(item)
    }

    fun itemContext(): SettingsItemContext = context

    /** Groups applicable settings by [SettingsGroup]. UI iterates this for rendering. */
    val groupedSettings by derivedStateOf {
        settingsVersion // read to establish dependency
        val applicable = settingsManager.applicableSettings()
        SettingsGroup.entries
            .map { group -> SettingsGroupedItems(group, applicable.filter { it.group == group }) }
            .filter { it.items.isNotEmpty() }
    }

    /** Active Settings diagnostic screen — all known items with resolved value + source. */
    val activeSettingsEntries by derivedStateOf {
        settingsVersion // read to establish dependency
        settingsManager.registry.all().map { item ->
            ActiveSettingsEntry(
                displayName = item.displayName,
                resolvedValue = formatResolvedValue(item),
                source = settingsManager.getSource(item),
                layerDetails = buildLayerDetails(item),
            )
        }
    }

    /** Convenience for hot-path code that wants the active brand id. */
    val activeBrandId: String
        get() {
            settingsVersion // read to establish dependency
            return getResolvedValue(ActiveBrandSetting)
        }

    /** Convenience for current startup tabs (used by the "Use current tabs" action). */
    val currentStartupTabs: List<String>
        get() {
            settingsVersion
            return getResolvedValue(StartupTabsSetting)
        }

    /** Typed read. Returns the registry default if no source has set the item. */
    fun <T : Any> getResolvedValue(item: SettingsItem<T>): T {
        settingsVersion // read to establish dependency
        return settingsManager.resolveValue(item)
    }

    /** Typed read returning null if no source has set the item (does NOT fall back to default). */
    fun <T : Any> getResolvedValueOrNull(item: SettingsItem<T>): T? {
        settingsVersion // read to establish dependency
        for (source in PRECEDENCE_CHAIN) {
            val value = settingsManager.getValueFromSource(item, source)
            if (value != null) {
                @Suppress("UNCHECKED_CAST")
                return value as T
            }
        }
        return null
    }

    fun isOverridden(item: SettingsItem<*>): Boolean = settingsManager.isOverridden(item)

    fun getSource(item: SettingsItem<*>): SettingsSource = settingsManager.getSource(item)

    fun getEffectiveSource(item: SettingsItem<*>): SettingsSource = settingsManager.getEffectiveSource(item)

    fun <T : Any> setUserSetting(item: SettingsItem<T>, value: T?) {
        settingsManager.setUserSetting(item, value)
        settingsVersion++
    }

    fun clearUserSetting(item: SettingsItem<*>) {
        settingsManager.clearUserSetting(item)
        settingsVersion++
    }

    fun saveCurrentTabsAsStartupTabs(tabSources: List<String>) {
        if (tabSources.isEmpty()) {
            settingsManager.clearUserSetting(StartupTabsSetting)
        } else {
            settingsManager.setUserSetting(StartupTabsSetting, tabSources)
        }
        settingsVersion++
    }

    fun clearAllUserSettings() {
        for (item in settingsManager.applicableSettings()) {
            settingsManager.clearUserSetting(item)
        }
        settingsVersion++
    }

    /** Describes the next-best source label when a user setting hides a lower-priority one. */
    fun fallbackSourceLabel(item: SettingsItem<*>): String {
        val effectiveSource = getEffectiveSource(item)
        if (effectiveSource != SettingsSource.USER_SETTINGS_FROM_SESSION &&
            effectiveSource != SettingsSource.USER_SETTINGS_FROM_PERSISTENCE) return ""
        var pastEffective = false
        for (source in PRECEDENCE_CHAIN) {
            if (source == effectiveSource) {
                pastEffective = true
                continue
            }
            if (!pastEffective) continue
            if (source == SettingsSource.USER_SETTINGS_FROM_SESSION ||
                source == SettingsSource.USER_SETTINGS_FROM_PERSISTENCE) continue
            val layerValue = settingsManager.getValueFromSource(item, source)
            if (layerValue != null) {
                return source.displayLabel(runtimeEnvironment).lowercase()
            }
        }
        return "default"
    }

    private fun buildLayerDetails(item: SettingsItem<*>): String {
        val lines = mutableListOf<String>()
        for (source in PRECEDENCE_CHAIN) {
            val layerValue = settingsManager.getValueFromSource(item, source)
            val label = source.displayLabel(settingsManager.runtimeEnvironment)
            val valueText = if (layerValue != null) formatRawValue(layerValue) else "—"
            lines.add("$label: $valueText")
        }
        return lines.joinToString("\n")
    }

    private fun formatResolvedValue(item: SettingsItem<*>): String {
        @Suppress("UNCHECKED_CAST")
        val typed = item as SettingsItem<Any>
        val value = settingsManager.resolveValue(typed)
        return formatRawValue(value)
    }

    private fun formatRawValue(value: Any?): String = when (value) {
        null -> "null"
        is Boolean -> value.toString()
        is String -> value
        is List<*> -> {
            val items = value.filterIsInstance<String>()
            if (items.isEmpty()) "(empty)" else items.joinToString(", ")
        }
        else -> value.toString()
    }
}

data class SettingsGroupedItems(
    val group: SettingsGroup,
    val items: List<SettingsItem<*>>,
)

data class ActiveSettingsEntry(
    val displayName: String,
    val resolvedValue: String,
    val tooltipText: String? = null,
    val source: SettingsSource,
    val layerDetails: String = "",
)
