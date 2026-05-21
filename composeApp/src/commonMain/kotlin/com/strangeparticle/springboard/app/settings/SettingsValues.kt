package com.strangeparticle.springboard.app.settings

/**
 * Holds the values for one source layer (defaults, persisted, env vars, CLI…).
 *
 * Backed by a map keyed by [SettingsItem.id]. Typed access goes through the
 * parameterized [get] / [withSetting] / [withRawSetting] methods — the public
 * API never leaks `Any?`. The unchecked cast inside [get] is sound because the
 * only insertion paths are [withSetting] (statically typed by `T`) and the
 * persistence boundary which goes through `item.deserialize` before insertion.
 */
data class SettingsValues(private val values: Map<String, Any?> = emptyMap()) {

    fun isSet(item: SettingsItem<*>): Boolean = values.containsKey(item.id)

    fun isSetById(id: String): Boolean = values.containsKey(id)

    /** Typed read. */
    fun <T : Any> get(item: SettingsItem<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return values[item.id] as T?
    }

    /** Typed read by id. Returns `Any?` because the id alone doesn't carry the type. */
    fun getById(id: String): Any? = values[id]

    /** Typed write. Setting to null removes the entry. */
    fun <T : Any> withSetting(item: SettingsItem<T>, value: T?): SettingsValues =
        copy(values = if (value == null) values - item.id else values + (item.id to value))

    /**
     * Type-erased write by id. Used at the persistence boundary, where the value
     * has just come out of `item.deserialize` and is known to be the right type
     * but the caller doesn't have the typed item handle in static context.
     */
    fun withRawSetting(id: String, value: Any?): SettingsValues =
        copy(values = if (value == null) values - id else values + (id to value))

    /** All currently-set ids (excludes unset entries). */
    fun setIds(): Set<String> = values.keys
}
