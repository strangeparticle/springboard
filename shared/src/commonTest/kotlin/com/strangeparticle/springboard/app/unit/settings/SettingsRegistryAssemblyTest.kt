package com.strangeparticle.springboard.app.unit.settings

import com.strangeparticle.springboard.app.luther.provider.AiProviderSettingsAdaptorRegistry
import com.strangeparticle.springboard.app.settings.SettingsRegistry
import com.strangeparticle.springboard.app.settings.items.core.ActiveBrandSetting
import com.strangeparticle.springboard.app.settings.items.core.coreSettingsItems
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SettingsRegistryAssemblyTest {

    @Test
    fun `registry exposes lookup by id`() {
        val registry = SettingsRegistry(coreSettingsItems())
        assertNotNull(registry.byId(ActiveBrandSetting.id))
        assertNull(registry.byId("does.not.exist"))
    }

    @Test
    fun `duplicate ids fail at construction`() {
        val original = coreSettingsItems()
        assertFailsWith<IllegalArgumentException> {
            SettingsRegistry(original + original)  // every id is duplicated
        }
    }

    @Test
    fun `provider items integrate alongside core items`() {
        val all = coreSettingsItems() + AiProviderSettingsAdaptorRegistry.allSettingsItems()
        val registry = SettingsRegistry(all)
        // Core items are present
        assertNotNull(registry.byId(ActiveBrandSetting.id))
        // Provider-owned items are present
        for (adaptor in AiProviderSettingsAdaptorRegistry.all()) {
            for (item in adaptor.settingsItems()) {
                assertNotNull(registry.byId(item.id), "missing ${item.id}")
            }
        }
    }
}
