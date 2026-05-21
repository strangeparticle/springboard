package com.strangeparticle.springboard.app.settings.items.base

import com.strangeparticle.springboard.app.settings.DropDownOption

/**
 * A string-valued setting whose value must be one of a fixed list of options.
 * Subclasses provide the [options] list at class-load time.
 *
 * For dropdowns whose options come from a live service call, use
 * [DropDownFromApiCallSettingsItem] instead.
 */
abstract class DropDownSettingsItem : StringSettingsItem() {
    abstract val options: List<DropDownOption>

    override fun coerceFromString(raw: String): String {
        val allowed = options.any { it.id == raw }
        if (!allowed) {
            val allowedIds = options.joinToString(", ") { it.id }
            throw IllegalArgumentException("'$raw' is not an allowed value (allowed: $allowedIds)")
        }
        return raw
    }
}
