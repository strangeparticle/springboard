package com.strangeparticle.springboard.app.unit.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.strangeparticle.springboard.app.ui.AppBottomBar
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.springboard.app.ui.brand.BrandRegistry
import com.strangeparticle.springboard.app.ui.brand.LocalUiBrand
import com.strangeparticle.springboard.app.ui.brand.infrastructure.BottomBarLogo
import kotlin.test.Test
import kotlin.test.assertEquals
import springboard.composeapp.generated.resources.Res
import springboard.composeapp.generated.resources.springboard_icon_512

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

    @Test
    fun brandLogoRendersWhenBrandProvidesOne() = runComposeUiTest {
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                val brandWithLogo = LocalUiBrand.current.copy(
                    drawableResources = LocalUiBrand.current.drawableResources.copy(
                        bottomBarLogo = BottomBarLogo(
                            drawable = Res.drawable.springboard_icon_512,
                            height = 20.dp,
                            startPadding = 6.dp,
                        ),
                    ),
                )
                CompositionLocalProvider(LocalUiBrand provides brandWithLogo) {
                    AppBottomBar(
                        isAssistantConfigured = true,
                        isAssistantOpen = false,
                        onToggleAssistant = {},
                        onOpenSettings = {},
                    )
                }
            }
        }
        onNodeWithTag(TestTags.BOTTOM_BAR_LOGO).assertExists()
    }

    @Test
    fun brandLogoAbsentWhenBrandHasNone() = runComposeUiTest {
        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                val brandWithoutLogo = LocalUiBrand.current.copy(
                    drawableResources = LocalUiBrand.current.drawableResources.copy(
                        bottomBarLogo = null,
                    ),
                )
                CompositionLocalProvider(LocalUiBrand provides brandWithoutLogo) {
                    AppBottomBar(
                        isAssistantConfigured = true,
                        isAssistantOpen = false,
                        onToggleAssistant = {},
                        onOpenSettings = {},
                    )
                }
            }
        }
        onNodeWithTag(TestTags.BOTTOM_BAR_LOGO).assertDoesNotExist()
    }
}
