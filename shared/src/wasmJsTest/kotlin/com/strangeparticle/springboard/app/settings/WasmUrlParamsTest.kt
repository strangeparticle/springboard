package com.strangeparticle.springboard.app.settings

import com.strangeparticle.springboard.app.settings.items.core.StartupTabsSetting
import kotlin.test.Test
import kotlin.test.assertEquals

class WasmUrlParamsTest {

    @Test
    fun `parseUrlParamSearch keeps decoded registered params and ignores unknown params`() {
        val registry = SettingsRegistry(listOf(StartupTabsSetting))

        val params = parseUrlParamSearch(
            "?startup-tabs=https%3A%2F%2Fexample.com%2Fspringboard.json&unknown=value",
            registry,
        )

        assertEquals(
            mapOf("startup-tabs" to "https://example.com/springboard.json"),
            params,
        )
    }
}
