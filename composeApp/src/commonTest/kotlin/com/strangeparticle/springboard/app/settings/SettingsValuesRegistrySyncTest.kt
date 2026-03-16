package com.strangeparticle.springboard.app.settings

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
    fun testEveryRegistrySettingHasASettingsValuesProperty() {
        val registryKeys = SettingsRegistry.allSettings().map { it.key }.toSet()
        for (key in registryKeys) {
            assertTrue(
                key.jsonKey in SettingsValues.settingsPropertyNames,
                "Registry setting $key (jsonKey=${key.jsonKey}) has no matching property in SettingsValues"
            )
        }
    }

    @Test
    fun testEverySettingsValuesPropertyHasARegistrySetting() {
        val registryJsonKeys = SettingsKey.entries.map { it.jsonKey }.toSet()
        for (propertyName in SettingsValues.settingsPropertyNames) {
            assertTrue(
                propertyName in registryJsonKeys,
                "SettingsValues property '$propertyName' has no matching setting in the registry"
            )
        }
    }

    @Test
    fun testSettingsValuesPropertyCountMatchesRegistryCount() {
        val registryCount = SettingsRegistry.allSettings().size
        val propertiesCount = SettingsValues.settingsPropertyNames.size
        assertEquals(
            registryCount, propertiesCount,
            "Registry has $registryCount settings but SettingsValues has $propertiesCount properties"
        )
    }

    @Test
    fun testEverySettingsKeyIsCoveredByGet() {
        // Verify that SettingsValues.get() handles every SettingsKey without throwing.
        // This catches a missing branch in the when() expression.
        val values = SettingsValues()
        for (key in SettingsKey.entries) {
            // Should not throw — just returns null for unset values
            values.get(key)
        }
    }

    @Test
    fun testEverySettingsKeyIsCoveredByWithSetting() {
        // Verify that SettingsValues.withSetting() handles every SettingsKey without throwing.
        var values = SettingsValues()
        for (key in SettingsKey.entries) {
            val entry = SettingsRegistry.require(key)
            val testValue = entry.defaultValue
            values = values.withSetting(key, testValue)
            assertEquals(testValue, values.get(key), "withSetting should store the value for $key")
        }
    }

    @Test
    fun testDefaultValueTypesMatchRegistryDeclarations() {
        for (item in SettingsRegistry.allSettings()) {
            val defaultValue = item.defaultValue
            if (defaultValue != null) {
                assertEquals(
                    item.type, defaultValue::class,
                    "Default value for ${item.key} has type ${defaultValue::class.simpleName} " +
                        "but registry declares type ${item.type.simpleName}"
                )
            } else {
                assertEquals(
                    FilePath::class, item.type,
                    "Null default only allowed for FilePath type, but ${item.key} is ${item.type.simpleName}"
                )
            }
        }
    }
}
