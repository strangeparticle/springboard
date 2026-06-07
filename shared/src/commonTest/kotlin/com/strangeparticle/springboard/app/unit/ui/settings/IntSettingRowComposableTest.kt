package com.strangeparticle.springboard.app.unit.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.strangeparticle.springboard.app.settings.items.core.HttpAiProviderTimeoutSecondsSetting
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.shared.stubHttpClientForTests
import com.strangeparticle.springboard.app.ui.brand.AppTheme
import com.strangeparticle.springboard.app.ui.brand.BrandRegistry
import com.strangeparticle.springboard.app.ui.settings.SettingRowComposable
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class IntSettingRowComposableTest {

    @Test
    fun `integer setting can be edited`() = runComposeUiTest {
        val settingsManager = createSettingsManagerForTest()
        val viewModel = SettingsViewModel(
            settingsManager = settingsManager,
            httpClient = stubHttpClientForTests(),
        )

        setContent {
            AppTheme(brandId = BrandRegistry.defaultBrand.id) {
                SettingRowComposable(HttpAiProviderTimeoutSecondsSetting, viewModel)
            }
        }

        onNodeWithText("AI Provider HTTP Timeout").assertIsDisplayed()
        val input = onNode(hasSetTextAction())
        input.performTextClearance()
        input.performTextInput("300")

        assertEquals(300, settingsManager.resolveValue(HttpAiProviderTimeoutSecondsSetting))
    }
}
