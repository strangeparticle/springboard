package com.strangeparticle.springboard.app.unit.settings

import com.strangeparticle.springboard.app.settings.SettingsGroup
import com.strangeparticle.springboard.app.settings.items.base.IntSettingsItem
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntSettingsItemTest {

    private object TestIntSetting : IntSettingsItem() {
        override val id = "test.int"
        override val displayName = "Test Int"
        override val description = "Test integer setting."
        override val group = SettingsGroup.DeveloperTools
        override val defaultValue = 5
        override val minimumValue = 1
        override val maximumValue = 10
    }

    @Test
    fun `serializes and deserializes integer values`() {
        assertEquals(JsonPrimitive(7), TestIntSetting.serialize(7))
        assertEquals(7, TestIntSetting.deserialize(JsonPrimitive(7)))
    }

    @Test
    fun `coerces valid raw string values`() {
        assertEquals(8, TestIntSetting.coerceFromString("8"))
    }

    @Test
    fun `rejects non-integer raw string values`() {
        assertFailsWith<IllegalArgumentException> {
            TestIntSetting.coerceFromString("slow")
        }
    }

    @Test
    fun `rejects values outside configured range`() {
        assertFailsWith<IllegalArgumentException> {
            TestIntSetting.coerceFromString("0")
        }
        assertFailsWith<IllegalArgumentException> {
            TestIntSetting.coerceFromString("11")
        }
    }
}
