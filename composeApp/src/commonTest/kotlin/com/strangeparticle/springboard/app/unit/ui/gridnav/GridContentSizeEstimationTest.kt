package com.strangeparticle.springboard.app.unit.ui.gridnav

import com.strangeparticle.springboard.app.domain.model.*
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.ui.gridnav.ActivatorPreviewHeightDp
import com.strangeparticle.springboard.app.ui.gridnav.GridZoomSelection
import com.strangeparticle.springboard.app.ui.gridnav.computeAvailableGridArea
import com.strangeparticle.springboard.app.ui.gridnav.computeZoomToFit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GridContentSizeEstimationTest {

    @Test
    fun computeAvailableGridArea_subtractsChromeFromViewport() {
        val (availableWidth, availableHeight) = computeAvailableGridArea(1000, 800)
        assertEquals(1000, availableWidth)
        val expectedChromeHeight = CommonUiConstants.NavbarHeight.value.toInt() +
            CommonUiConstants.StatusBarHeight.value.toInt() +
            ActivatorPreviewHeightDp
        assertEquals(800 - expectedChromeHeight, availableHeight)
    }

    @Test
    fun computeAvailableGridArea_widthPassedThrough() {
        val (availableWidth, _) = computeAvailableGridArea(500, 600)
        assertEquals(500, availableWidth)
    }

    @Test
    fun computeZoomToFit_returnsConservativePreset() {
        val springboard = makeSmallSpringboard()
        // With a large viewport the content fits easily — should return a reasonable preset
        val result = computeZoomToFit(2000, 2000, springboard)
        assertTrue(result is GridZoomSelection.FixedZoom)
    }

    @Test
    fun computeZoomToFit_withTinyViewport_returnsLowestPreset() {
        val springboard = makeSmallSpringboard()
        // Viewport smaller than content at any zoom level
        val result = computeZoomToFit(10, 10, springboard)
        assertTrue(result is GridZoomSelection.FixedZoom)
        assertEquals(100, result.percent) // lowest preset
    }

    @Test
    fun computeZoomToFit_withSmallContent_returnsHighPreset() {
        // Empty apps/resources still has positive natural size (padding + header height),
        // so with a large viewport it selects a high zoom preset.
        val springboard = makeSpringboard(apps = emptyList(), resources = emptyList())
        val result = computeZoomToFit(1000, 800, springboard)
        assertTrue(result is GridZoomSelection.FixedZoom)
        assertTrue(result.percent >= 100)
    }

    private fun makeSmallSpringboard(): Springboard = makeSpringboard(
        apps = listOf(App("app1", "App One"), App("app2", "App Two")),
        resources = listOf(Resource("res1", "Resource 1"), Resource("res2", "Resource 2")),
    )

    private fun makeSpringboard(
        apps: List<App>,
        resources: List<Resource>,
    ): Springboard = Springboard(
        name = "Test",
        environments = listOf(Environment("env1", "Env 1")),
        apps = apps,
        resources = resources,
        activators = emptyList(),
        guidanceData = emptyList(),
        displayHints = null,
        indexes = SpringboardIndexes(
            activatorByCoordinate = emptyMap(),
            activatableResourcesByApp = emptyMap(),
            activatableAppsByResource = emptyMap(),
            activatableResourcesByEnvApp = emptyMap(),
        ),
        source = "test",
        lastLoadTime = 0L,
        jsonSource = "{}",
    )
}
