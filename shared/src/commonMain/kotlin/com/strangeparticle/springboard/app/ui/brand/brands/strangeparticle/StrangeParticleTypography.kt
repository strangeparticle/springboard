package com.strangeparticle.springboard.app.ui.brand.brands.strangeparticle

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.Font
import springboard.composeapp.generated.resources.InterVariable
import springboard.composeapp.generated.resources.Res

@Composable
fun StrangeParticleTypography(): Typography {
    val interFontFamily = FontFamily(Font(Res.font.InterVariable))

    // this gives us baseline TextStyle references, allowing us to just override fontFamily but get
    // default values for all other TextStyle properties like size, weight, etc
    val baselineDefaults = Typography()

    return Typography(
        displayLarge = baselineDefaults.displayLarge.copy(fontFamily = interFontFamily),
        displayMedium = baselineDefaults.displayMedium.copy(fontFamily = interFontFamily),
        displaySmall = baselineDefaults.displaySmall.copy(fontFamily = interFontFamily),
        headlineLarge = baselineDefaults.headlineLarge.copy(fontFamily = interFontFamily),
        headlineMedium = baselineDefaults.headlineMedium.copy(fontFamily = interFontFamily),
        headlineSmall = baselineDefaults.headlineSmall.copy(fontFamily = interFontFamily),
        titleLarge = baselineDefaults.titleLarge.copy(fontFamily = interFontFamily),
        titleMedium = baselineDefaults.titleMedium.copy(fontFamily = interFontFamily),
        titleSmall = baselineDefaults.titleSmall.copy(fontFamily = interFontFamily),
        bodyLarge = baselineDefaults.bodyLarge.copy(fontFamily = interFontFamily),
        bodyMedium = baselineDefaults.bodyMedium.copy(fontFamily = interFontFamily),
        bodySmall = baselineDefaults.bodySmall.copy(fontFamily = interFontFamily),
        labelLarge = baselineDefaults.labelLarge.copy(fontFamily = interFontFamily),
        labelMedium = baselineDefaults.labelMedium.copy(fontFamily = interFontFamily),
        labelSmall = baselineDefaults.labelSmall.copy(fontFamily = interFontFamily),
    )
}
