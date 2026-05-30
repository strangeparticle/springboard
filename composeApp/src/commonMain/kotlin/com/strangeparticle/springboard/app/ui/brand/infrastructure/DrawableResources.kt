package com.strangeparticle.springboard.app.ui.brand.infrastructure

import org.jetbrains.compose.resources.DrawableResource

data class DrawableResources(
    val appLogo: DrawableResource,
    // Brands without a bottom-bar logo leave this null; the bottom bar then renders no logo.
    val bottomBarLogo: BottomBarLogo? = null,
)
