package com.strangeparticle.springboard.app.ui.brand.brands.strangeparticle

import androidx.compose.ui.unit.dp
import com.strangeparticle.springboard.app.ui.brand.infrastructure.BottomBarLogo
import com.strangeparticle.springboard.app.ui.brand.infrastructure.DrawableResources
import springboard.composeapp.generated.resources.Res
import springboard.composeapp.generated.resources.sp_logotype_black_type
import springboard.composeapp.generated.resources.sp_logotype_white_type
import springboard.composeapp.generated.resources.springboard_icon_512

// The bottom-bar logotype reads as type against the bar background, so each theme uses the
// variant that contrasts: black type on the light bar, white type on the dark bar.
private val StrangeParticleBlackTypeBottomBarLogo = BottomBarLogo(
    drawable = Res.drawable.sp_logotype_black_type,
    height = 20.dp,
    startPadding = 8.dp,
)

private val StrangeParticleWhiteTypeBottomBarLogo = BottomBarLogo(
    drawable = Res.drawable.sp_logotype_white_type,
    height = 20.dp,
    startPadding = 8.dp,
)

val StrangeParticleImagePaintersLightTheme = DrawableResources(
    appLogo = Res.drawable.springboard_icon_512,
    bottomBarLogo = StrangeParticleBlackTypeBottomBarLogo,
)

val StrangeParticleImagePaintersDarkTheme = DrawableResources(
    appLogo = Res.drawable.springboard_icon_512,
    bottomBarLogo = StrangeParticleWhiteTypeBottomBarLogo,
)
