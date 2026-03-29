package com.strangeparticle.springboard.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.Font
import springboard.composeapp.generated.resources.InterVariable
import springboard.composeapp.generated.resources.Res

@Composable
fun appTypography(): Typography {
    val interFontFamily = FontFamily(Font(Res.font.InterVariable))
    val defaultTypography = Typography()
    return Typography(
        displayLarge = defaultTypography.displayLarge.copy(fontFamily = interFontFamily),
        displayMedium = defaultTypography.displayMedium.copy(fontFamily = interFontFamily),
        displaySmall = defaultTypography.displaySmall.copy(fontFamily = interFontFamily),
        headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = interFontFamily),
        headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = interFontFamily),
        headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = interFontFamily),
        titleLarge = defaultTypography.titleLarge.copy(fontFamily = interFontFamily),
        titleMedium = defaultTypography.titleMedium.copy(fontFamily = interFontFamily),
        titleSmall = defaultTypography.titleSmall.copy(fontFamily = interFontFamily),
        bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = interFontFamily),
        bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = interFontFamily),
        bodySmall = defaultTypography.bodySmall.copy(fontFamily = interFontFamily),
        labelLarge = defaultTypography.labelLarge.copy(fontFamily = interFontFamily),
        labelMedium = defaultTypography.labelMedium.copy(fontFamily = interFontFamily),
        labelSmall = defaultTypography.labelSmall.copy(fontFamily = interFontFamily),
    )
}
