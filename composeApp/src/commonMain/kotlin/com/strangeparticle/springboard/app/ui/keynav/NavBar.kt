package com.strangeparticle.springboard.app.ui.keynav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.strangeparticle.springboard.app.ui.theme.CommonUiConstants
import com.strangeparticle.springboard.app.ui.theme.color.NavbarBackground
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel

@Composable
fun NavBar(
    viewModel: SpringboardViewModel,
    firstDropdownFocusRequester: FocusRequester
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(CommonUiConstants.NavbarHeight)
            .background(NavbarBackground)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LogoAndName(modifier = Modifier.align(Alignment.CenterVertically))

        Spacer(modifier = Modifier.weight(1f))

        if (viewModel.isConfigLoaded) {
            KeyNav(
                viewModel = viewModel,
                firstDropdownFocusRequester = firstDropdownFocusRequester
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        VersionDesignator()
    }
}
