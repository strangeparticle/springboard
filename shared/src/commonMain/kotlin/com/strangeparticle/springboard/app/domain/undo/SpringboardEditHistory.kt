package com.strangeparticle.springboard.app.domain.undo

import com.strangeparticle.springboard.app.domain.model.Springboard

/**
 * Maintains a bounded list of [Springboard] snapshots for a single tab, supporting
 * undo and redo.
 *
 * Invariants:
 *   - [cursor] always indexes the currently-applied snapshot within [snapshots].
 *   - [savedMarker] indexes the snapshot that was last written to disk, or null when
 *     that snapshot is no longer retained (it was pushed out by the cap, or was on a
 *     redo branch that was discarded by a subsequent [record]).
 */
class SpringboardEditHistory(initial: Springboard, private val maxEntries: Int = 100) {

    init {
        require(maxEntries >= 1) { "maxEntries must be at least 1" }
    }

    private val snapshots: MutableList<Springboard> = mutableListOf(initial)
    private var cursor: Int = 0
    private var savedMarker: Int? = 0

    val current: Springboard
        get() = snapshots[cursor]

    val canUndo: Boolean
        get() = cursor > 0

    val canRedo: Boolean
        get() = cursor < snapshots.lastIndex

    val isDirty: Boolean
        get() = cursor != savedMarker

    fun record(springboard: Springboard) {
        // Drop all snapshots ahead of the cursor (the redo branch).
        while (snapshots.lastIndex > cursor) {
            snapshots.removeAt(snapshots.lastIndex)
        }

        // If the saved state was on the discarded redo branch, it is no longer retained.
        val marker = savedMarker
        if (marker != null && marker > cursor) {
            savedMarker = null
        }

        snapshots.add(springboard)
        cursor = snapshots.lastIndex

        enforceCap()
    }

    fun undo(): Springboard? {
        if (!canUndo) {
            return null
        }
        cursor -= 1
        return current
    }

    fun redo(): Springboard? {
        if (!canRedo) {
            return null
        }
        cursor += 1
        return current
    }

    fun markSaved() {
        savedMarker = cursor
    }

    private fun enforceCap() {
        while (snapshots.size > maxEntries) {
            snapshots.removeAt(0)
            cursor -= 1
            if (savedMarker != null) {
                val decremented = savedMarker!! - 1
                savedMarker = if (decremented < 0) null else decremented
            }
        }
    }
}
