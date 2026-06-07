package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.ui.input.pointer.PointerIcon

/**
 * Platform-provided pointer icon shown while hovering the grid header resize thumb.
 * Desktop returns a vertical (north/south) resize cursor; other platforms fall back
 * to the default crosshair-style icon.
 */
expect val gridHeaderVerticalResizePointerIcon: PointerIcon
