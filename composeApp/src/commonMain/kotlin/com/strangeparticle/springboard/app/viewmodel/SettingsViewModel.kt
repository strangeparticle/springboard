package com.strangeparticle.springboard.app.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.strangeparticle.springboard.app.settings.*

/**
 * Manages Settings screen state and delegates reads and writes to the [SettingsManager].
 *
 * This keeps the pattern consistent with the existing [SpringboardViewModel].
 */
class SettingsViewModel(
    private val settingsManager: SettingsManager,
) : ViewModel() {

    val runtimeEnvironment: RuntimeEnvironment = settingsManager.runtimeEnvironment

    /**
     * A counter that increments on every user setting change,
     * used to trigger recomposition of settings-dependent UI.
     */
    var settingsVersion by mutableStateOf(0)
        private set

    /**
     * Returns the applicable settings for the current runtime environment,
     * grouped by their category for UI rendering.
     */
    val groupedSettings by derivedStateOf {
        settingsVersion // read to establish dependency
        val applicable = settingsManager.applicableSettings()
        val general = applicable.filter { it !is SettingItem.Desktop }
        val desktopOsx = applicable.filter { item ->
            item is SettingItem.Desktop && RuntimeEnvironment.DesktopOsx in item.runtimeEnvironments
        }
        buildList {
            if (general.isNotEmpty()) add(SettingsGroup("General", general))
            if (desktopOsx.isNotEmpty()) add(SettingsGroup("Desktop macOS", desktopOsx))
        }
    }

    /**
     * Returns all settings (not just applicable) with their resolved values
     * and sources, for the Active Settings diagnostic screen.
     */
    val activeSettingsEntries by derivedStateOf {
        settingsVersion // read to establish dependency
        SettingsRegistry.allSettings().map { item ->
            val valueDisplay = formatValue(item, settingsManager.resolveValue(item.key))
            val layerDetails = buildLayerDetails(item)
            ActiveSettingsEntry(
                displayName = item.displayName,
                resolvedValue = valueDisplay.displayText,
                tooltipText = valueDisplay.tooltipText,
                source = settingsManager.getSource(item.key),
                layerDetails = layerDetails,
            )
        }
    }

    /**
     * The currently resolved active-brand id, reading through [settingsVersion]
     * so that callers recompose when the setting changes.
     */
    val activeBrandId: String
        get() {
            settingsVersion // read to establish dependency
            return settingsManager.getSelectedOptionIdFromDropDown(SettingsKey.ACTIVE_BRAND)
        }

    fun getResolvedValue(key: SettingsKey): Any? {
        settingsVersion // read to establish dependency
        return settingsManager.resolveValue(key)
    }

    fun isOverridden(key: SettingsKey): Boolean =
        settingsManager.isOverridden(key)

    fun getSource(key: SettingsKey): SettingsSource =
        settingsManager.getSource(key)

    fun getEffectiveSource(key: SettingsKey): SettingsSource =
        settingsManager.getEffectiveSource(key)

    fun setUserSetting(key: SettingsKey, value: Any?) {
        settingsManager.setUserSetting(key, value)
        settingsVersion++
    }

    fun clearUserSetting(key: SettingsKey) {
        settingsManager.setUserSetting(key, null)
        settingsVersion++
    }

    fun saveCurrentTabsAsStartupTabs(tabSources: List<String>) {
        if (tabSources.isEmpty()) {
            settingsManager.setUserSetting(SettingsKey.STARTUP_TABS, null)
        } else {
            settingsManager.setUserSetting(SettingsKey.STARTUP_TABS, tabSources)
        }
        settingsVersion++
    }

    fun clearAllUserSettings() {
        for (item in settingsManager.applicableSettings()) {
            settingsManager.setUserSetting(item.key, null)
        }
        settingsVersion++
    }

    fun fallbackSourceLabel(key: SettingsKey): String {
        val effectiveSource = getEffectiveSource(key)
        if (effectiveSource != SettingsSource.USER_SETTINGS_FROM_SESSION &&
            effectiveSource != SettingsSource.USER_SETTINGS_FROM_PERSISTENCE) return ""
        var pastEffective = false
        for (source in PRECEDENCE_CHAIN) {
            if (source == effectiveSource) {
                pastEffective = true
                continue
            }
            if (!pastEffective) continue
            if (source == SettingsSource.USER_SETTINGS_FROM_SESSION || source == SettingsSource.USER_SETTINGS_FROM_PERSISTENCE) continue
            val layerValue = settingsManager.getValueFromSource(key, source)
            if (layerValue != null) {
                return source.displayLabel(runtimeEnvironment).lowercase()
            }
        }
        return "default"
    }

    private fun buildLayerDetails(item: SettingItem): String {
        val lines = mutableListOf<String>()
        for (source in PRECEDENCE_CHAIN) {
            val layerValue = settingsManager.getValueFromSource(item.key, source)
            val label = formatSourceLabelForLayer(source)
            val valueText = if (layerValue != null) formatValue(item, layerValue).displayText else "—"
            lines.add("$label: $valueText")
        }
        return lines.joinToString("\n")
    }

    private fun formatSourceLabelForLayer(source: SettingsSource): String =
        source.displayLabel(settingsManager.runtimeEnvironment)

    private fun formatValue(item: SettingItem, value: Any?): ActiveSettingsValueDisplay {
        if (item.type == StringFromDropDown::class) {
            val id = value as? String
            val declaration = item.defaultValue as? StringFromDropDown
            val displayText = id?.let { declaration?.displayNameFor(it) } ?: id ?: "null"
            return ActiveSettingsValueDisplay(displayText = displayText)
        }
        return when (value) {
            null -> ActiveSettingsValueDisplay(displayText = "null")
            is FilePath -> ActiveSettingsValueDisplay(displayText = "path", tooltipText = value.path)
            is List<*> -> {
                val items = value.filterIsInstance<String>()
                if (items.isEmpty()) ActiveSettingsValueDisplay(displayText = "(empty)")
                else ActiveSettingsValueDisplay(displayText = items.joinToString(", "))
            }
            else -> ActiveSettingsValueDisplay(displayText = value.toString())
        }
    }
}

data class ActiveSettingsValueDisplay(
    val displayText: String,
    val tooltipText: String? = null,
)

data class SettingsGroup(
    val name: String,
    val settings: List<SettingItem>,
)

data class ActiveSettingsEntry(
    val displayName: String,
    val resolvedValue: String,
    val tooltipText: String? = null,
    val source: SettingsSource,
    val layerDetails: String = "",
)
