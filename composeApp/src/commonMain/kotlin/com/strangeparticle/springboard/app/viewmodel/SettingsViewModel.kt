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
    private val currentFilePath: () -> String?,
) : ViewModel() {

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
            ActiveSettingsEntry(
                displayName = item.displayName,
                resolvedValue = valueDisplay.displayText,
                tooltipText = valueDisplay.tooltipText,
                source = settingsManager.getSource(item.key),
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

    fun setUserSetting(key: SettingsKey, value: Any?) {
        settingsManager.setUserSetting(key, value)
        settingsVersion++
    }

    /**
     * Designates the currently open springboard file as the startup springboard.
     * Returns true if successful, false if no file is currently open.
     */
    fun designateCurrentFileAsStartup(): Boolean {
        val path = currentFilePath() ?: return false
        setUserSetting(SettingsKey.STARTUP_SPRINGBOARD, FilePath(path))
        return true
    }

    /**
     * Clears the startup springboard designation.
     */
    fun clearStartupSpringboard() {
        setUserSetting(SettingsKey.STARTUP_SPRINGBOARD, null)
    }

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
)
