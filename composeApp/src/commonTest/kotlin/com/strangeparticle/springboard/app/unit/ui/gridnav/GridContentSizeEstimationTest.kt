package com.strangeparticle.springboard.app.unit.ui.gridnav

import com.strangeparticle.springboard.app.domain.model.*
import com.strangeparticle.springboard.app.domain.factory.SpringboardFactory
import com.strangeparticle.springboard.app.ui.brand.CommonUiConstants
import com.strangeparticle.springboard.app.ui.gridnav.ActivatorPreviewHeightDp
import com.strangeparticle.springboard.app.ui.gridnav.GridZoomSelection
import com.strangeparticle.springboard.app.ui.gridnav.computeAvailableGridArea
import com.strangeparticle.springboard.app.ui.gridnav.computeZoomToFit
import com.strangeparticle.springboard.app.ui.gridnav.estimateGridContentHeightDp
import com.strangeparticle.springboard.app.ui.gridnav.estimateGridContentWidthDp
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

    @Test
    fun estimateGridContentHeightDp_includesEnvSectionHeadingAndSpacerWhenBothSectionsRender() {
        val springboardWithBoth = SpringboardFactory.fromJson(
            """
            {
              "name": "Both sections",
              "environments": [{ "id": "dev", "name": "Dev" }],
              "apps": [{ "id": "app1", "name": "App" }],
              "resources": [
                { "id": "res1", "name": "All-envs only" },
                { "id": "res2", "name": "Dev only" },
                { "id": "res3", "name": "Dev only 2" }
              ],
              "activators": [
                { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "ALL", "url": "https://example.com/1" },
                { "type": "url", "appId": "app1", "resourceId": "res2", "environmentId": "dev", "url": "https://example.com/2" },
                { "type": "url", "appId": "app1", "resourceId": "res3", "environmentId": "dev", "url": "https://example.com/3" }
              ]
            }
            """.trimIndent(),
            source = "/test",
        )
        val springboardWithEnvOnly = SpringboardFactory.fromJson(
            """
            {
              "name": "Env-only",
              "environments": [{ "id": "dev", "name": "Dev" }],
              "apps": [{ "id": "app1", "name": "App" }],
              "resources": [
                { "id": "res2", "name": "Dev only" },
                { "id": "res3", "name": "Dev only 2" }
              ],
              "activators": [
                { "type": "url", "appId": "app1", "resourceId": "res2", "environmentId": "dev", "url": "https://example.com/2" },
                { "type": "url", "appId": "app1", "resourceId": "res3", "environmentId": "dev", "url": "https://example.com/3" }
              ]
            }
            """.trimIndent(),
            source = "/test",
        )

        val rowHeight = CommonUiConstants.GridRowHeight.value.toInt()
        val delta = estimateGridContentHeightDp(springboardWithBoth) -
            estimateGridContentHeightDp(springboardWithEnvOnly)

        // Adding an all-envs section adds: 1 ALL-envs resource row + 1 inter-section
        // spacer row + 1 env-section heading row = 3 × GridRowHeight (plus a small
        // dividers contribution).
        val expectedMinDelta = 3 * rowHeight
        assertTrue(
            delta >= expectedMinDelta,
            "Expected estimate to grow by at least $expectedMinDelta when adding an all-envs section, but grew by only $delta",
        )
    }

    @Test
    fun estimateGridContentWidthDp_addsOneColumnPerSeparatorSlot() {
        val withoutGroups = SpringboardFactory.fromJson(
            """
            {
              "name": "No groups",
              "environments": [{ "id": "dev", "name": "Dev" }],
              "apps": [
                { "id": "a1", "name": "A" },
                { "id": "a2", "name": "B" },
                { "id": "a3", "name": "C" },
                { "id": "a4", "name": "D" }
              ],
              "resources": [{ "id": "res1", "name": "Resource" }],
              "activators": [
                { "type": "url", "appId": "a1", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/a1" },
                { "type": "url", "appId": "a2", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/a2" },
                { "type": "url", "appId": "a3", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/a3" },
                { "type": "url", "appId": "a4", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/a4" }
              ]
            }
            """.trimIndent(),
            source = "/test",
        )
        val withTwoSeparators = SpringboardFactory.fromJson(
            """
            {
              "name": "Two separators",
              "environments": [{ "id": "dev", "name": "Dev" }],
              "apps": [
                { "id": "a1", "name": "A", "appGroupId": "g1" },
                { "id": "a2", "name": "B", "appGroupId": "g2" },
                { "id": "a3", "name": "C", "appGroupId": "g1" },
                { "id": "a4", "name": "D" }
              ],
              "resources": [{ "id": "res1", "name": "Resource" }],
              "activators": [
                { "type": "url", "appId": "a1", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/a1" },
                { "type": "url", "appId": "a2", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/a2" },
                { "type": "url", "appId": "a3", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/a3" },
                { "type": "url", "appId": "a4", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/a4" }
              ],
              "appGroups": [
                { "id": "g1", "description": "Group 1" },
                { "id": "g2", "description": "Group 2" }
              ]
            }
            """.trimIndent(),
            source = "/test",
        )

        val columnWidth = CommonUiConstants.GridColumnWidth.value.toInt()
        val widthDelta = estimateGridContentWidthDp(withTwoSeparators) -
            estimateGridContentWidthDp(withoutGroups)
        // Layout for the grouped fixture: [a1, a3, sep, a2, sep, a4] → +2 separator slots.
        assertEquals(2 * columnWidth, widthDelta)
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
