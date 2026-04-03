package com.strangeparticle.springboard.app

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel

private const val NavbarHeightDp = 56
private const val StatusBarHeightDp = 32
private const val GridCellHeightDp = 40
private const val GridTopPaddingDp = 16
private const val GridBottomPaddingDp = 16
private const val EnvironmentTitleHeightDp = 40
private const val WindowVerticalBufferDp = 40

private const val ResourceLabelWidthDp = 200
private const val AppColumnWidthDp = 40
private const val GridHorizontalPaddingDp = 72

private const val NavbarDropdownCount = 3
private const val NavbarDropdownWidthDp = 100
private const val NavbarDropdownGapDp = 8
private const val NavbarLogoAndPaddingBufferDp = 250
private const val NavbarSafetyMarginFactor = 1.1

private const val MaxHeaderCharCount = 20
private const val HeaderTextAvgCharWidthDp = 7.5f
private const val HeaderTextHeightDp = 18f
private const val Sin45 = 0.7071f

fun calculateWindowWidth(springboard: Springboard): Int {
    val longestAppNameLength = springboard.apps.maxOfOrNull { minOf(it.name.length, MaxHeaderCharCount) } ?: 0
    val estimatedHeaderTextWidthDp = longestAppNameLength * HeaderTextAvgCharWidthDp
    val headerDiagonalOverhangDp = ((estimatedHeaderTextWidthDp + HeaderTextHeightDp) * Sin45).toInt()

    val gridWidth = ResourceLabelWidthDp +
        (springboard.apps.size * AppColumnWidthDp) +
        headerDiagonalOverhangDp +
        GridHorizontalPaddingDp
    val navbarMinWidth = (
        (NavbarDropdownCount * NavbarDropdownWidthDp +
        (NavbarDropdownCount - 1) * NavbarDropdownGapDp +
        NavbarLogoAndPaddingBufferDp) * NavbarSafetyMarginFactor
    ).toInt()
    return maxOf(gridWidth, navbarMinWidth)
}

fun calculateWindowHeight(springboard: Springboard): Int {
    val longestAppNameLength = springboard.apps.maxOfOrNull { minOf(it.name.length, MaxHeaderCharCount) } ?: 0
    val estimatedHeaderTextWidthDp = longestAppNameLength * HeaderTextAvgCharWidthDp
    val estimatedHeaderHeightDp = ((estimatedHeaderTextWidthDp + HeaderTextHeightDp) * Sin45).toInt()

    return NavbarHeightDp +
        GridTopPaddingDp +
        EnvironmentTitleHeightDp +
        estimatedHeaderHeightDp +
        (springboard.resources.size * GridCellHeightDp) +
        GridBottomPaddingDp +
        StatusBarHeightDp +
        WindowVerticalBufferDp
}

fun resizeWindowToFitSpringboard(viewModel: SpringboardViewModel, windowState: WindowState) {
    val springboard = viewModel.springboard ?: return
    if (springboard.displayHints != null) {
        val width = springboard.displayHints.width
        val height = springboard.displayHints.height
        if (width != null && height != null) {
            windowState.size = DpSize(width.dp, height.dp)
            windowState.position = WindowPosition(Alignment.Center)
        }
        return
    }
    windowState.size = DpSize(
        width = calculateWindowWidth(springboard).dp,
        height = calculateWindowHeight(springboard).dp
    )
    windowState.position = WindowPosition(Alignment.Center)
}
