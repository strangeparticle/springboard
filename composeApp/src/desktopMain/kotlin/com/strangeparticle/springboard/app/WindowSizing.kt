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
import com.strangeparticle.springboard.app.ui.gridnav.percent
import com.strangeparticle.springboard.app.ui.gridnav.estimateGridContentWidthDp
import com.strangeparticle.springboard.app.ui.gridnav.estimateGridContentHeightDp
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import java.awt.Toolkit

private const val WindowVerticalBufferDp = 16

// Navbar minimum width estimation
private const val NavbarDropdownCount = 3
private const val NavbarDropdownWidthDp = 100
private const val NavbarDropdownGapDp = 8
private const val NavbarLogoAndPaddingBufferDp = 250
private const val NavbarSafetyMarginFactor = 1.1

private const val ScreenUsableMarginFactor = 0.9

fun calculateWindowWidth(springboard: Springboard, zoomPercent: Int = 100): Int {
    val gridWidth = estimateGridContentWidthDp(springboard) * zoomPercent / 100
    val navbarMinWidth = (
        (NavbarDropdownCount * NavbarDropdownWidthDp +
        (NavbarDropdownCount - 1) * NavbarDropdownGapDp +
        NavbarLogoAndPaddingBufferDp) * NavbarSafetyMarginFactor
    ).toInt()
    return maxOf(gridWidth, navbarMinWidth)
}

fun calculateWindowHeight(springboard: Springboard, zoomPercent: Int = 100): Int {
    val navbarHeightDp = CommonUiConstants.NavbarHeight.value.toInt()
    val statusBarHeightDp = CommonUiConstants.StatusBarHeight.value.toInt()
    val tabBarHeightDp = CommonUiConstants.TabBarHeight.value.toInt()
    val bottomBarHeightDp = CommonUiConstants.BottomBarHeight.value.toInt()
    val gridContentHeight = estimateGridContentHeightDp(springboard) * zoomPercent / 100

    return navbarHeightDp +
        gridContentHeight +
        ActivatorPreviewHeightDp +
        statusBarHeightDp +
        tabBarHeightDp +
        bottomBarHeightDp +
        WindowVerticalBufferDp
}

fun resizeWindowToFitSpringboard(viewModel: SpringboardViewModel, windowState: WindowState) {
    val springboardFilteredForRuntime = viewModel.springboardFilteredForRuntime ?: return

    val contentWidth = calculateWindowWidth(springboardFilteredForRuntime)
    val contentHeight = calculateWindowHeight(springboardFilteredForRuntime)

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
    val tabsWithContent = viewModel.tabs.filter { it.springboardFilteredForRuntime != null }
    if (tabsWithContent.isEmpty()) return

    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val maxScreenWidth = (screenSize.width * ScreenUsableMarginFactor).toInt()
    val maxScreenHeight = (screenSize.height * ScreenUsableMarginFactor).toInt()

    var neededWidth = windowState.size.width.value.toInt()
    var neededHeight = windowState.size.height.value.toInt()

    for (tab in tabsWithContent) {
        val springboardFilteredForRuntime = tab.springboardFilteredForRuntime!!
        val zoomPercent = tab.gridZoomSelection.percent
        neededWidth = maxOf(neededWidth, calculateWindowWidth(springboardFilteredForRuntime, zoomPercent))
        neededHeight = maxOf(neededHeight, calculateWindowHeight(springboardFilteredForRuntime, zoomPercent))
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
