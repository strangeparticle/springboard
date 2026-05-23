package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.ui.input.pointer.PointerIcon

/**
 * Platform-provided pointer icon shown while hovering the resource column resize thumb.
 * Desktop returns a horizontal east/west resize cursor; other platforms fall back to the
 * default crosshair-style icon.
 */
expect val gridColumnHorizontalResizePointerIcon: PointerIcon
