package com.strangeparticle.springboard.app.ui.keynav

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.AppVersion
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.theme.color.NavbarText

@Composable
fun VersionDesignator() {
    Text(
        text = "v${AppVersion.VERSION}",
        color = NavbarText.copy(alpha = 0.6f),
        fontSize = 12.sp,
        modifier = Modifier.testTag(TestTags.VERSION_DESIGNATOR)
    )
}
