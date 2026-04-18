package com.strangeparticle.springboard.app.unit.settings

import com.strangeparticle.springboard.app.settings.SettingsKey
import com.strangeparticle.springboard.app.settings.SettingsKeyNaming
import com.strangeparticle.springboard.app.settings.SettingsRegistry
import com.strangeparticle.springboard.app.settings.SettingsValues
import com.strangeparticle.springboard.app.settings.StringFromDropDown

import kotlin.test.*

/**
 * Verifies that [SettingsRegistry] and [SettingsValues] remain in sync.
 *
 * These tests should fail loudly if the two structures drift apart —
 * for example, if a setting is added to the registry without a matching
 * property in [SettingsValues], or vice versa.
 */
class SettingsValuesRegistrySyncTest {

    @Test
    fun `every registry setting has a settings values property`() {
        val registryKeys = SettingsRegistry.allSettings().map { it.key }.toSet()
        for (key in registryKeys) {
            assertTrue(
                SettingsKeyNaming.jsonKey(key) in SettingsValues.settingsPropertyNames,
                "Registry setting $key (jsonKey=${SettingsKeyNaming.jsonKey(key)}) has no matching property in SettingsValues"
            )
        }
    }

    @Test
    fun `every settings values property has a registry setting`() {
        val registryJsonKeys = SettingsKey.entries.map { SettingsKeyNaming.jsonKey(it) }.toSet()
        for (propertyName in SettingsValues.settingsPropertyNames) {
            assertTrue(
                propertyName in registryJsonKeys,
                "SettingsValues property '$propertyName' has no matching setting in the registry"
            )
        }
    }

    @Test
    fun `settings values property count matches registry count`() {
        val registryCount = SettingsRegistry.allSettings().size
        val propertiesCount = SettingsValues.settingsPropertyNames.size
        assertEquals(
            registryCount, propertiesCount,
            "Registry has $registryCount settings but SettingsValues has $propertiesCount properties"
        )
    }

    @Test
    fun `every settings key is covered by get`() {
        // Verify that SettingsValues.get() handles every SettingsKey without throwing.
        // This catches a missing branch in the when() expression.
        val values = SettingsValues()
        for (key in SettingsKey.entries) {
            // Should not throw — just returns null for unset values
            values.get(key)
        }
    }

    @Test
    fun `every settings key is covered by withSetting`() {
        // Verify that SettingsValues.withSetting() handles every SettingsKey without throwing.
        // For StringFromDropDown-typed settings, the registry default is a declaration
        // (options + default id), but the per-layer stored value is the plain id String,
        // so we extract that here to mirror what SettingsManager.initDefaultSettings does.
        var values = SettingsValues()
        for (key in SettingsKey.entries) {
            val entry = SettingsRegistry.require(key)
            val testValue: Any? = if (entry.type == StringFromDropDown::class) {
                (entry.defaultValue as StringFromDropDown).defaultDropDownOptionId
            } else {
                entry.defaultValue
            }
            values = values.withSetting(key, testValue)
            assertEquals(testValue, values.get(key), "withSetting should store the value for $key")
        }
    }

    @Test
    fun `default value types match registry declarations`() {
        for (item in SettingsRegistry.allSettings()) {
            val defaultValue = item.defaultValue
            if (defaultValue != null) {
                if (item.type == List::class) {
                    assertTrue(
                        defaultValue is List<*>,
                        "Default value for ${item.key} should be a List but was ${defaultValue::class.simpleName}"
                    )
                } else {
                    assertEquals(
                        item.type, defaultValue::class,
                        "Default value for ${item.key} has type ${defaultValue::class.simpleName} " +
                            "but registry declares type ${item.type.simpleName}"
                    )
                }
            } else {
                fail("Null default is not allowed — ${item.key} (${item.type.simpleName}) has a null default value")
            }
        }
    }
}
