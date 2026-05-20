package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.domain.factory.SpringboardFactory
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.ui.tabs.TabStatusIcon
import com.strangeparticle.springboard.app.ui.tabs.tabStatusIconsFor
import com.strangeparticle.springboard.app.viewmodel.TabState
import com.strangeparticle.springboard.app.ui.gridnav.GridZoomSelection
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [tabStatusIconFor], which decides whether a tab title shows the
 * dirty indicator, the lock glyph, both indicators, or nothing.
 *
 * Rules per spec §2.3:
 * - dirty local-file or source-less tab → Dirty
 * - clean + non-saveable source (HTTP / S3) → Lock
 * - dirty + non-saveable source (HTTP / S3) → Lock + Dirty
 * - clean local-file → null
 * - empty (`source == null` or `springboard == null`) → null
 */
class TabStatusIconTest {

    private val springboard = SpringboardFactory.fromJson(TestFixtureJson.URL_ONLY, "/test.json")

    @Test
    fun `clean local-file tab shows no status icon`() {
        assertEquals(emptyList(), tabStatusIconsFor(loadedTab(source = "/path/to/file.json", isDirty = false)))
    }

    @Test
    fun `dirty local-file tab shows the Dirty status icon`() {
        assertEquals(
            listOf(TabStatusIcon.Dirty),
            tabStatusIconsFor(loadedTab(source = "/path/to/file.json", isDirty = true)),
        )
    }

    @Test
    fun `clean http tab shows the NonSaveable status icon`() {
        assertEquals(
            listOf(TabStatusIcon.NonSaveable),
            tabStatusIconsFor(loadedTab(source = "https://example.com/sb.json", isDirty = false)),
        )
    }

    @Test
    fun `clean http URL with mixed-case scheme is recognised as non-saveable`() {
        assertEquals(
            listOf(TabStatusIcon.NonSaveable),
            tabStatusIconsFor(loadedTab(source = "HTTPS://example.com/sb.json", isDirty = false)),
        )
    }

    @Test
    fun `clean s3 tab shows the NonSaveable status icon`() {
        assertEquals(
            listOf(TabStatusIcon.NonSaveable),
            tabStatusIconsFor(loadedTab(source = "s3://bucket/key.json", isDirty = false)),
        )
    }

    @Test
    fun `dirty http tab shows NonSaveable and Dirty icons`() {
        assertEquals(
            listOf(TabStatusIcon.NonSaveable, TabStatusIcon.Dirty),
            tabStatusIconsFor(loadedTab(source = "https://example.com/sb.json", isDirty = true)),
        )
    }

    @Test
    fun `dirty s3 tab shows NonSaveable and Dirty icons`() {
        assertEquals(
            listOf(TabStatusIcon.NonSaveable, TabStatusIcon.Dirty),
            tabStatusIconsFor(loadedTab(source = "s3://bucket/key.json", isDirty = true)),
        )
    }

    @Test
    fun `dirty source-less tab shows only the Dirty icon`() {
        assertEquals(
            listOf(TabStatusIcon.Dirty),
            tabStatusIconsFor(loadedTab(source = null, isDirty = true)),
        )
    }

    @Test
    fun `empty tab with null source shows no status icon`() {
        assertEquals(emptyList(), tabStatusIconsFor(TabState.createEmpty("tab-1")))
    }

    @Test
    fun `tab with non-null source but no springboard loaded shows no status icon`() {
        // A tab that was opened with a path but failed to load — no springboard yet.
        assertEquals(
            emptyList(),
            tabStatusIconsFor(
                TabState.createEmpty("tab-1").copy(source = "/path/to/file.json", springboard = null),
            )
        )
    }

    private fun loadedTab(source: String?, isDirty: Boolean): TabState =
        TabState(
            tabId = "tab-1",
            label = "Test",
            source = source,
            springboard = springboard,
            selectedEnvironmentId = null,
            selectedAppId = null,
            selectedResourceId = null,
            multiSelectSet = emptySet(),
            hoveredActivatorPreview = null,
            gridZoomSelection = GridZoomSelection.default(),
            isLoading = false,
            isDirty = isDirty,
        )
}
