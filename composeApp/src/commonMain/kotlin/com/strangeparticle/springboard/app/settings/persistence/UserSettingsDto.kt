package com.strangeparticle.springboard.app.settings.persistence

import com.strangeparticle.springboard.app.settings.FilePath
import com.strangeparticle.springboard.app.settings.StringFromDropDown
import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsRegistry
import com.strangeparticle.springboard.app.settings.SettingsValues
import kotlinx.serialization.Serializable

/**
 * DTO for persisted user settings. This structure exists specifically for
 * serialization/deserialization and is separate from the runtime [SettingsValues].
 *
 * Fields use camelCase names matching the JSON key convention.
 * All fields are nullable — absent fields mean the user has not set that value.
 *
 * This separation keeps the runtime model clean while giving persistence a
 * dedicated place to handle JSON shape, missing fields, explicit nulls,
 * and future migration concerns.
 */
@Serializable
data class UserSettingsDto(
    val startupSpringboard: String? = null,
    val openUrlsInNewWindowSingle: Boolean? = null,
    val openUrlsInNewWindowMultiple: Boolean? = null,
    val surfaceAppleScriptErrors: Boolean? = null,
    val resetKeyNavAfterKeyNavActivation: Boolean? = null,
    val resetKeyNavAfterGridNavActivation: Boolean? = null,
    val activeBrand: String? = null,
) {
    /** Converts this DTO to a [SettingsValues] instance. */
    fun toSettingsValues(): SettingsValues {
        var values = SettingsValues()
        if (startupSpringboard != null) {
            values = values.withSetting(SettingsKey.STARTUP_SPRINGBOARD, FilePath(startupSpringboard))
        }
        if (openUrlsInNewWindowSingle != null) {
            values = values.withSetting(SettingsKey.OPEN_URLS_IN_NEW_WINDOW_SINGLE, openUrlsInNewWindowSingle)
        }
        if (openUrlsInNewWindowMultiple != null) {
            values = values.withSetting(SettingsKey.OPEN_URLS_IN_NEW_WINDOW_MULTIPLE, openUrlsInNewWindowMultiple)
        }
        if (surfaceAppleScriptErrors != null) {
            values = values.withSetting(SettingsKey.SURFACE_APPLESCRIPT_ERRORS, surfaceAppleScriptErrors)
        }
        if (resetKeyNavAfterKeyNavActivation != null) {
            values = values.withSetting(SettingsKey.RESET_KEY_NAV_AFTER_KEY_NAV_ACTIVATION, resetKeyNavAfterKeyNavActivation)
        }
        if (resetKeyNavAfterGridNavActivation != null) {
            values = values.withSetting(SettingsKey.RESET_KEY_NAV_AFTER_GRID_NAV_ACTIVATION, resetKeyNavAfterGridNavActivation)
        }
        if (activeBrand != null) {
            val declaration = SettingsRegistry.require(SettingsKey.ACTIVE_BRAND).defaultValue as StringFromDropDown
            if (declaration.isAllowed(activeBrand)) {
                values = values.withSetting(SettingsKey.ACTIVE_BRAND, activeBrand)
            }
        }
        return values
    }

    companion object {
        /** Creates a DTO from the user-settings layer of a [SettingsValues] instance. */
        fun fromSettingsValues(values: SettingsValues): UserSettingsDto {
            return UserSettingsDto(
                startupSpringboard = values.startupSpringboard?.path,
                openUrlsInNewWindowSingle = values.openUrlsInNewWindowSingle,
                openUrlsInNewWindowMultiple = values.openUrlsInNewWindowMultiple,
                surfaceAppleScriptErrors = values.surfaceApplescriptErrors,
                resetKeyNavAfterKeyNavActivation = values.resetKeyNavAfterKeyNavActivation,
                resetKeyNavAfterGridNavActivation = values.resetKeyNavAfterGridNavActivation,
                activeBrand = values.activeBrand,
            )
        }
    }
}
