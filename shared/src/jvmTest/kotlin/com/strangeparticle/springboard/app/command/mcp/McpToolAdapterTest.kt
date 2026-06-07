package com.strangeparticle.springboard.app.command.mcp

import com.strangeparticle.luther.toolcall.ToolCallExecutionResult
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class McpToolAdapterTest {
    @Test
    fun `tool schema extracts properties and required`() {
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") { putJsonObject("tab_id") { put("type", "string") } }
            put("required", buildJsonArray { add("tab_id") })
        }
        val toolSchema = toToolSchema(schema)
        assertTrue(toolSchema.properties?.containsKey("tab_id") == true)
        assertEquals(listOf("tab_id"), toolSchema.required)
    }

    @Test
    fun `success response maps to non-error result`() {
        val result = toCallToolResult(ToolCallExecutionResult(success = true, message = "done"))
        assertEquals(false, result.isError ?: false)
    }

    @Test
    fun `failure response maps to error result`() {
        val result = toCallToolResult(
            ToolCallExecutionResult(success = false, message = "nope", code = "bad"),
        )
        assertEquals(true, result.isError)
    }
}
