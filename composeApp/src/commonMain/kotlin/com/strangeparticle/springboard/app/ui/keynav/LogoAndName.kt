package com.strangeparticle.springboard.app.ui.keynav

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.theme.color.NavbarText
import org.jetbrains.compose.resources.painterResource
import springboard.composeapp.generated.resources.Res
import springboard.composeapp.generated.resources.springboard_icon_512

@Composable
fun LogoAndName(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.springboard_icon_512),
        contentDescription = "Springboard",
        modifier = Modifier.size(36.dp).offset(y = 6.dp).testTag(TestTags.SPRINGBOARD_ICON).then(modifier)
    )
    Spacer(modifier = Modifier.width(10.dp))
    Text(
        text = "Springboard",
        color = NavbarText,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}
