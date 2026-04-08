package com.strangeparticle.springboard.app.settings

/**
 * Declaration of a string-typed setting whose value must be one of a fixed
 * list of allowed options, rendered as a dropdown in the UI.
 *
 * This is a *declaration*, not a value: it lives on [SettingItem.defaultValue]
 * to describe the available options and which one is the registry-supplied
 * default. The currently-resolved value of the setting (after applying user
 * settings, env vars, and CLI args) is stored separately as a plain `String`
 * in the source layers — see [SettingsValues].
 *
 * The [SettingItem.type] for any setting using this is `StringFromDropDown::class`,
 * which the rendering layer dispatches on to draw a dropdown.
 */
data class StringFromDropDown(
    val defaultDropDownOptionId: String,
    val dropDownOptions: List<DropDownOption>,
) {
    val defaultDropDownOption: DropDownOption?
        get() = dropDownOptions.firstOrNull { it.id == defaultDropDownOptionId }

    fun isAllowed(id: String): Boolean = dropDownOptions.any { it.id == id }

    fun displayNameFor(id: String): String? =
        dropDownOptions.firstOrNull { it.id == id }?.displayName
}

/**
 * A single selectable option within a [StringFromDropDown]. The [id] is what
 * gets persisted / passed via CLI/env; the [displayName] is shown in the UI.
 */
data class DropDownOption(
    val id: String,
    val displayName: String,
)
