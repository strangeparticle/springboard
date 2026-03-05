package com.strangeparticle.springboard.app.ui.keynav

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.ui.theme.NavbarText
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel

@Composable
fun VersionDesignator() {
    Text(
        text = "v${SpringboardViewModel.VERSION}",
        color = NavbarText.copy(alpha = 0.6f),
        fontSize = 12.sp
    )
}
