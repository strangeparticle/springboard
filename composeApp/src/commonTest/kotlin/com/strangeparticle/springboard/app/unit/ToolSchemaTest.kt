package com.strangeparticle.springboard.app.unit

import com.strangeparticle.luther.toolcall.ToolFieldDescription
import com.strangeparticle.luther.toolcall.enumValues
import com.strangeparticle.luther.toolcall.requestSchema
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ToolSchemaTest {

    @Serializable
    private enum class SampleMode {
        FAST,
        SAFE,
    }

    @Serializable
    private data class NestedRequest(
        @ToolFieldDescription("Nested id.")
        val nested_id: String,
    )

    @Serializable
    private data class SampleRequest(
        @ToolFieldDescription("Primary id.")
        val id: String,
        @ToolFieldDescription("Optional note.")
        val note: String? = null,
        @ToolFieldDescription("Enabled flag.")
        val enabled: Boolean,
        @ToolFieldDescription("Selected mode.")
        val mode: SampleMode,
        @ToolFieldDescription("Nested payload.")
        val nested: NestedRequest,
        @ToolFieldDescription("List of tags.")
        val tags: List<String>,
    )

    @Test
    fun `requestSchema derives schema from serializable request metadata`() {
        val s = requestSchema(SampleRequest.serializer())

        assertEquals("object", (s["type"] as JsonPrimitive).content)
        val props = s["properties"] as JsonObject
        assertEquals(listOf("id", "note", "enabled", "mode", "nested", "tags"), props.keys.toList())

        val required = (s["required"] as JsonArray).map { (it as JsonPrimitive).content }
        assertEquals(listOf("id", "enabled", "mode", "nested", "tags"), required)

        val idSchema = props["id"] as JsonObject
        assertEquals("string", (idSchema["type"] as JsonPrimitive).content)
        assertEquals("Primary id.", (idSchema["description"] as JsonPrimitive).content)

        val noteSchema = props["note"] as JsonObject
        assertEquals("string", (noteSchema["type"] as JsonPrimitive).content)
        assertEquals("Optional note.", (noteSchema["description"] as JsonPrimitive).content)

        val enabledSchema = props["enabled"] as JsonObject
        assertEquals("boolean", (enabledSchema["type"] as JsonPrimitive).content)

        val modeSchema = props["mode"] as JsonObject
        assertEquals("string", (modeSchema["type"] as JsonPrimitive).content)
        assertEquals(listOf("FAST", "SAFE"), (modeSchema["enum"] as JsonArray).map { (it as JsonPrimitive).content })
        assertEquals(listOf("FAST", "SAFE"), modeSchema.enumValues()!!.map { (it as JsonPrimitive).content })

        val nestedSchema = props["nested"] as JsonObject
        assertEquals("object", (nestedSchema["type"] as JsonPrimitive).content)
        val nestedProps = nestedSchema["properties"] as JsonObject
        val nestedIdSchema = nestedProps["nested_id"] as JsonObject
        assertEquals("Nested id.", (nestedIdSchema["description"] as JsonPrimitive).content)

        val tagsSchema = props["tags"] as JsonObject
        assertEquals("array", (tagsSchema["type"] as JsonPrimitive).content)
        val tagItems = tagsSchema["items"] as JsonObject
        assertEquals("string", (tagItems["type"] as JsonPrimitive).content)
    }
}
