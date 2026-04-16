package com.strangeparticle.springboard.app

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.ui.gridnav.ActivatorPreviewHeightDp
import com.strangeparticle.springboard.app.ui.gridnav.GridZoomSelection
import com.strangeparticle.springboard.app.ui.gridnav.estimateGridContentWidthDp
import com.strangeparticle.springboard.app.ui.gridnav.estimateGridContentHeightDp
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import java.awt.Toolkit

private const val WindowVerticalBufferDp = 30

// Navbar minimum width estimation
private const val NavbarDropdownCount = 3
private const val NavbarDropdownWidthDp = 100
private const val NavbarDropdownGapDp = 8
private const val NavbarLogoAndPaddingBufferDp = 250
private const val NavbarSafetyMarginFactor = 1.1

private const val ScreenUsableMarginFactor = 0.9

fun calculateWindowWidth(springboard: Springboard): Int {
    val gridWidth = estimateGridContentWidthDp(springboard)
    val navbarMinWidth = (
        (NavbarDropdownCount * NavbarDropdownWidthDp +
        (NavbarDropdownCount - 1) * NavbarDropdownGapDp +
        NavbarLogoAndPaddingBufferDp) * NavbarSafetyMarginFactor
    ).toInt()
    return maxOf(gridWidth, navbarMinWidth)
}

fun calculateWindowHeight(springboard: Springboard): Int {
    val navbarHeightDp = CommonUiConstants.NavbarHeight.value.toInt()
    val statusBarHeightDp = CommonUiConstants.StatusBarHeight.value.toInt()
    val tabBarHeightDp = 32
    val gridContentHeight = estimateGridContentHeightDp(springboard)

    return navbarHeightDp +
        gridContentHeight +
        ActivatorPreviewHeightDp +
        statusBarHeightDp +
        tabBarHeightDp +
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
            viewModel.gridZoomSelection = GridZoomSelection.FixedZoom(100)
            return
        }
    }

    val contentWidth = calculateWindowWidth(springboard)
    val contentHeight = calculateWindowHeight(springboard)

    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val maxScreenWidth = (screenSize.width * ScreenUsableMarginFactor).toInt()
    val maxScreenHeight = (screenSize.height * ScreenUsableMarginFactor).toInt()

    windowState.size = DpSize(
        minOf(contentWidth, maxScreenWidth).dp,
        minOf(contentHeight, maxScreenHeight).dp,
    )
    viewModel.gridZoomSelection = GridZoomSelection.FixedZoom(100)
    windowState.position = WindowPosition(Alignment.Center)
}

fun growWindowToFitLargestTab(viewModel: SpringboardViewModel, windowState: WindowState) {
    val allSpringboards = viewModel.tabs.mapNotNull { it.springboard }
    if (allSpringboards.isEmpty()) return

    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val maxScreenWidth = (screenSize.width * ScreenUsableMarginFactor).toInt()
    val maxScreenHeight = (screenSize.height * ScreenUsableMarginFactor).toInt()

    var neededWidth = windowState.size.width.value.toInt()
    var neededHeight = windowState.size.height.value.toInt()

    for (springboard in allSpringboards) {
        if (springboard.displayHints != null) {
            val hintWidth = springboard.displayHints.width
            val hintHeight = springboard.displayHints.height
            if (hintWidth != null && hintHeight != null) {
                neededWidth = maxOf(neededWidth, hintWidth)
                neededHeight = maxOf(neededHeight, hintHeight)
                continue
            }
        }
        neededWidth = maxOf(neededWidth, calculateWindowWidth(springboard))
        neededHeight = maxOf(neededHeight, calculateWindowHeight(springboard))
    }

    val finalWidth = minOf(neededWidth, maxScreenWidth)
    val finalHeight = minOf(neededHeight, maxScreenHeight)
    val newSize = DpSize(finalWidth.dp, finalHeight.dp)

    if (newSize.width > windowState.size.width || newSize.height > windowState.size.height) {
        windowState.size = DpSize(
            maxOf(newSize.width, windowState.size.width),
            maxOf(newSize.height, windowState.size.height),
        )
        windowState.position = WindowPosition(Alignment.Center)
    }
}
