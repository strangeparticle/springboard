package com.strangeparticle.springboard.app.unit.settings

import com.strangeparticle.springboard.app.settings.persistence.SettingsSerializer
import com.strangeparticle.springboard.app.settings.persistence.UserSettingsDto
import kotlin.test.*

class SettingsSerializerTest {

    @Test
    fun testRoundTripBooleans() {
        val original = UserSettingsDto(
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
    fun testRoundTripFilePath() {
        val original = UserSettingsDto(startupSpringboard = "/some/file.json")
        val json = SettingsSerializer.toJson(original)
        val restored = SettingsSerializer.fromJson(json)

        assertNotNull(restored)
        assertEquals("/some/file.json", restored.startupSpringboard)
    }

    @Test
    fun testUnsetFieldsAreOmitted() {
        val dto = UserSettingsDto(surfaceAppleScriptErrors = true)
        val json = SettingsSerializer.toJson(dto)

        // Only the set field should appear in JSON (encodeDefaults = false)
        assertFalse(json.contains("startupSpringboard"), "Unset fields should not appear in JSON")
        assertTrue(json.contains("surfaceAppleScriptErrors"))
    }

    @Test
    fun testMissingFieldsDefaultToNull() {
        val json = """{"surfaceAppleScriptErrors": true}"""
        val restored = SettingsSerializer.fromJson(json)

        assertNotNull(restored)
        assertEquals(true, restored.surfaceAppleScriptErrors)
        assertNull(restored.startupSpringboard)
        assertNull(restored.openUrlsInNewWindowSingle)
    }

    @Test
    fun testInvalidJsonReturnsNull() {
        val result = SettingsSerializer.fromJson("not valid json")
        assertNull(result)
    }

    @Test
    fun testEmptyJsonObjectReturnsAllNulls() {
        val result = SettingsSerializer.fromJson("{}")
        assertNotNull(result)
        assertNull(result.startupSpringboard)
        assertNull(result.surfaceAppleScriptErrors)
    }

    @Test
    fun testUnknownFieldsAreIgnored() {
        val json = """{"unknownField": "value", "surfaceAppleScriptErrors": true}"""
        val restored = SettingsSerializer.fromJson(json)

        assertNotNull(restored)
        assertEquals(true, restored.surfaceAppleScriptErrors)
    }

    @Test
    fun testOutputIsPrettyPrinted() {
        val dto = UserSettingsDto(surfaceAppleScriptErrors = true)
        val json = SettingsSerializer.toJson(dto)
        assertTrue(json.contains("\n"), "Output should be pretty-printed")
    }
}
