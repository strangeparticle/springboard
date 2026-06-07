package com.strangeparticle.springboard.app.unit.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.ui.MainScreen
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.springboard.app.ui.brand.BrandRegistry
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MainScreenReloadTest {

    @Test
    fun `reload button resets when reload throws`() = runComposeUiTest {
        val viewModel = SpringboardViewModel(
            settingsManager = createSettingsManagerForTest(),
            persistenceService = PersistenceServiceInMemoryFake(),
        )
        viewModel.loadConfig(TestFixtureJson.URL_ONLY, "/test/springboard.json")

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                MainScreen(
                    viewModel = viewModel,
                    isShiftHeld = false,
                    onOpenSettings = {},
                )
            }
        }
        waitForIdle()

        onNodeWithTag(TestTags.RELOAD_BUTTON).performClick()
        mainClock.advanceTimeBy(CommonUiConstants.ReloadSpinMinMs + 100)
        waitForIdle()

        onNodeWithTag(TestTags.RELOAD_BUTTON).assertIsEnabled()
    }
}
