package com.strangeparticle.springboard.app.unit.settings

import com.strangeparticle.springboard.app.settings.persistence.SettingsSerializer
import com.strangeparticle.springboard.app.settings.persistence.SettingsDto
import kotlin.test.*

class SettingsSerializerTest {

    @Test
    fun `round trip booleans`() {
        val original = SettingsDto(
            surfaceAppleScriptErrors = true,
            openUrlsInNewWindowSingle = false,
        )
        val json = SettingsSerializer.toJson(original)
        val restored = SettingsSerializer.fromJson(json)

        assertNotNull(restored)
        assertEquals(true, restored.surfaceAppleScriptErrors)
        assertEquals(false, restored.openUrlsInNewWindowSingle)
    }

    @Test
    fun `round trip file path`() {
        val original = SettingsDto(startupSpringboard = "/some/file.json")
        val json = SettingsSerializer.toJson(original)
        val restored = SettingsSerializer.fromJson(json)

        assertNotNull(restored)
        assertEquals("/some/file.json", restored.startupSpringboard)
    }

    @Test
    fun `unset fields are omitted from json`() {
        val dto = SettingsDto(surfaceAppleScriptErrors = true)
        val json = SettingsSerializer.toJson(dto)

        // Only the set field should appear in JSON (encodeDefaults = false)
        assertFalse(json.contains("startupSpringboard"), "Unset fields should not appear in JSON")
        assertTrue(json.contains("surfaceAppleScriptErrors"))
    }

    @Test
    fun `missing fields default to null`() {
        val json = """{"surfaceAppleScriptErrors": true}"""
        val restored = SettingsSerializer.fromJson(json)

        assertNotNull(restored)
        assertEquals(true, restored.surfaceAppleScriptErrors)
        assertNull(restored.startupSpringboard)
        assertNull(restored.openUrlsInNewWindowSingle)
    }

    @Test
    fun `invalid json throws`() {
        assertFailsWith<IllegalArgumentException> {
            SettingsSerializer.fromJson("not valid json")
        }
    }

    @Test
    fun `empty json object returns all nulls`() {
        val result = SettingsSerializer.fromJson("{}")
        assertNotNull(result)
        assertNull(result.startupSpringboard)
        assertNull(result.surfaceAppleScriptErrors)
    }

    @Test
    fun `unknown fields are ignored`() {
        val json = """{"unknownField": "value", "surfaceAppleScriptErrors": true}"""
        val restored = SettingsSerializer.fromJson(json)

        assertNotNull(restored)
        assertEquals(true, restored.surfaceAppleScriptErrors)
    }

    @Test
    fun `output is pretty printed`() {
        val dto = SettingsDto(surfaceAppleScriptErrors = true)
        val json = SettingsSerializer.toJson(dto)
        assertTrue(json.contains("\n"), "Output should be pretty-printed")
    }
}
