package com.strangeparticle.springboard.app.ui.brand.infrastructure

import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.resources.DrawableResource

/**
 * A brand's logo for the lower-left corner of the bottom bar. The bottom bar is short
 * (see [com.strangeparticle.springboard.app.ui.brand.CommonUiConstants.BottomBarHeight]),
 * so each brand sizes its own asset via [height].
 *
 * Only [startPadding] (the leading gap from the window edge) is configurable: the logo is
 * shorter than the bar and centers vertically, so vertical spacing emerges from that
 * centering rather than from a declared padding.
 */
data class BottomBarLogo(
    val drawable: DrawableResource,
    val height: Dp,
    val startPadding: Dp,
)
