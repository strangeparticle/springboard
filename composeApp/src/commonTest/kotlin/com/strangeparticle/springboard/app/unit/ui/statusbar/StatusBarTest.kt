package com.strangeparticle.springboard.app.unit.ui.statusbar

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import com.strangeparticle.springboard.app.domain.factory.SpringboardFactory
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.springboard.app.ui.brand.BrandRegistry
import com.strangeparticle.springboard.app.ui.statusbar.StatusBar
import com.strangeparticle.springboard.app.viewmodel.TabState
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class StatusBarTest {

    @Test
    fun unsavedSpringboardShowsUnsavedPlaceholderInSourceArea() = runComposeUiTest {
        val springboard = SpringboardFactory.createEmpty("Untitled")
        val activeTab = TabState.createEmpty("tab-1").copy(
            source = null,
            springboardFilteredForRuntime = springboard,
            springboardUnfiltered = springboard,
        )

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                StatusBar(
                    activeTab = activeTab,
                    isReloading = false,
                    onReload = {},
                )
            }
        }

        onNodeWithTag(TestTags.STATUS_BAR_SOURCE).assertTextEquals("<unsaved>")
    }

    @Test
    fun openFromNetworkButtonTooltipSpecifiesCurrentTab() = runComposeUiTest {
        val springboard = SpringboardFactory.fromJson(TestFixtureJson.URL_ONLY, "/test/springboard.json")
        val activeTab = TabState.createEmpty("tab-1").copy(
            source = "/test/springboard.json",
            springboardFilteredForRuntime = springboard,
            springboardUnfiltered = springboard,
        )

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                StatusBar(
                    activeTab = activeTab,
                    isReloading = false,
                    onReload = {},
                    onOpenFromNetwork = {},
                )
            }
        }

        mainClock.autoAdvance = false
        onNodeWithTag(TestTags.OPEN_FROM_NETWORK_BUTTON).performMouseInput {
            moveTo(center)
        }
        mainClock.advanceTimeBy(1_000)

        onNodeWithText("Open from network in current tab").assertExists()
    }
}
