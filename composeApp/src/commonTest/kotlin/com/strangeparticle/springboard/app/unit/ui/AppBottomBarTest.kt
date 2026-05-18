package com.strangeparticle.springboard.app.unit.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.strangeparticle.springboard.app.ui.AppBottomBar
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.springboard.app.ui.brand.BrandRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AppBottomBarTest {

    @Test
    fun settingsGearTriggersOpenSettings() = runComposeUiTest {
        var settingsCount = 0
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AppBottomBar(
                    isAssistantConfigured = true,
                    isAssistantOpen = false,
                    onToggleAssistant = {},
                    onOpenSettings = { settingsCount += 1 },
                )
            }
        }
        onNodeWithTag(TestTags.SETTINGS_GEAR_ICON).performClick()
        assertEquals(1, settingsCount)
    }

    @Test
    fun assistantIconTriggersToggleAssistant() = runComposeUiTest {
        var toggleCount = 0
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                AppBottomBar(
                    isAssistantConfigured = false,
                    isAssistantOpen = false,
                    onToggleAssistant = { toggleCount += 1 },
                    onOpenSettings = {},
                )
            }
        }
        onNodeWithTag(TestTags.ASSISTANT_TOGGLE_BUTTON).performClick()
        assertEquals(1, toggleCount)
    }
}
