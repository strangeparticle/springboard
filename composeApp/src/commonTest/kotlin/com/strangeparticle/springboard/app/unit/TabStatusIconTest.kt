package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.domain.factory.SpringboardFactory
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.ui.tabs.TabStatusIcon
import com.strangeparticle.springboard.app.ui.tabs.tabStatusIconFor
import com.strangeparticle.springboard.app.viewmodel.TabState
import com.strangeparticle.springboard.app.ui.gridnav.GridZoomSelection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [tabStatusIconFor], which decides whether a tab title shows the
 * dirty asterisk, the lock glyph, or nothing.
 *
 * Rules per spec §2.3:
 * - dirty (any source) → Asterisk
 * - clean + non-saveable source (HTTP / S3) → Lock
 * - clean local-file → null
 * - empty (`source == null` or `springboard == null`) → null
 */
class TabStatusIconTest {

    private val springboard = SpringboardFactory.fromJson(TestFixtureJson.URL_ONLY, "/test.json")

    @Test
    fun `clean local-file tab shows no status icon`() {
        assertNull(tabStatusIconFor(loadedTab(source = "/path/to/file.json", isDirty = false)))
    }

    @Test
    fun `dirty local-file tab shows the Dirty status icon`() {
        assertEquals(
            TabStatusIcon.Dirty,
            tabStatusIconFor(loadedTab(source = "/path/to/file.json", isDirty = true)),
        )
    }

    @Test
    fun `clean http tab shows the NonSaveable status icon`() {
        assertEquals(
            TabStatusIcon.NonSaveable,
            tabStatusIconFor(loadedTab(source = "https://example.com/sb.json", isDirty = false)),
        )
    }

    @Test
    fun `clean http URL with mixed-case scheme is recognised as non-saveable`() {
        assertEquals(
            TabStatusIcon.NonSaveable,
            tabStatusIconFor(loadedTab(source = "HTTPS://example.com/sb.json", isDirty = false)),
        )
    }

    @Test
    fun `clean s3 tab shows the NonSaveable status icon`() {
        assertEquals(
            TabStatusIcon.NonSaveable,
            tabStatusIconFor(loadedTab(source = "s3://bucket/key.json", isDirty = false)),
        )
    }

    @Test
    fun `dirty http tab shows the Dirty icon (Dirty wins over NonSaveable)`() {
        assertEquals(
            TabStatusIcon.Dirty,
            tabStatusIconFor(loadedTab(source = "https://example.com/sb.json", isDirty = true)),
        )
    }

    @Test
    fun `dirty s3 tab shows the Dirty icon (Dirty wins over NonSaveable)`() {
        assertEquals(
            TabStatusIcon.Dirty,
            tabStatusIconFor(loadedTab(source = "s3://bucket/key.json", isDirty = true)),
        )
    }

    @Test
    fun `empty tab with null source shows no status icon`() {
        assertNull(tabStatusIconFor(TabState.createEmpty("tab-1")))
    }

    @Test
    fun `tab with non-null source but no springboard loaded shows no status icon`() {
        // A tab that was opened with a path but failed to load — no springboard yet.
        assertNull(
            tabStatusIconFor(
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
