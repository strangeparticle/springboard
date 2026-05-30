package com.strangeparticle.springboard.app.unit.ui.brand

import com.strangeparticle.springboard.app.ui.brand.brands.strangeparticle.StrangeParticleImagePaintersDarkTheme
import com.strangeparticle.springboard.app.ui.brand.brands.strangeparticle.StrangeParticleImagePaintersLightTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import springboard.composeapp.generated.resources.Res
import springboard.composeapp.generated.resources.sp_logotype_black_type
import springboard.composeapp.generated.resources.sp_logotype_white_type

class StrangeParticleImagePaintersTest {

    @Test
    fun lightThemeBottomBarLogoUsesBlackType() {
        assertEquals(
            Res.drawable.sp_logotype_black_type,
            StrangeParticleImagePaintersLightTheme.bottomBarLogo?.drawable,
        )
    }

    @Test
    fun darkThemeBottomBarLogoUsesWhiteType() {
        assertEquals(
            Res.drawable.sp_logotype_white_type,
            StrangeParticleImagePaintersDarkTheme.bottomBarLogo?.drawable,
        )
    }
}
