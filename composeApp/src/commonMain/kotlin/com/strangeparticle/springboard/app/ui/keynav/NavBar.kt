package com.strangeparticle.springboard.app.ui.keynav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.ui.brand.LocalUiBrand
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel

@Composable
fun NavBar(
    viewModel: SpringboardViewModel,
) {
    val currentUiBrand = LocalUiBrand.current
    Row(
        modifier = Modifier.fillMaxWidth().height(CommonUiConstants.NavbarHeight)
            .background(currentUiBrand.customColors.navbarBackground)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LogoAndName(modifier = Modifier.align(Alignment.CenterVertically))

        Spacer(modifier = Modifier.width(16.dp))

        if (viewModel.isConfigLoaded) {
            KeyNav(
                viewModel = viewModel,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.width(16.dp))

        VersionDesignator()
    }
}
