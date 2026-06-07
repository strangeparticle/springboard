package com.strangeparticle.springboard.app.settings

/**
 * A single selectable option in a dropdown setting. The [id] is what gets
 * persisted; the [displayName] is shown in the UI.
 */
data class DropDownOption(
    val id: String,
    val displayName: String,
)
