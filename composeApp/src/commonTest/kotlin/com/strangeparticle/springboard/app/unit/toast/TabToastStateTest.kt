package com.strangeparticle.springboard.app.unit.toast

import com.strangeparticle.springboard.app.ui.toast.TabToastState
import com.strangeparticle.springboard.app.ui.toast.ToastSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TabToastStateTest {

    @Test
    fun errorAddsToastWithErrorSeverity() {
        val state = TabToastState()
        state.error("something broke")
        assertEquals(1, state.activeToasts.size)
        assertEquals(ToastSeverity.ERROR, state.activeToasts.first().severity)
        assertEquals("something broke", state.activeToasts.first().message)
    }

    @Test
    fun warningAddsToastWithWarningSeverity() {
        val state = TabToastState()
        state.warning("be careful")
        assertEquals(ToastSeverity.WARNING, state.activeToasts.first().severity)
    }

    @Test
    fun infoAddsToastWithInfoSeverity() {
        val state = TabToastState()
        state.info("all good")
        assertEquals(ToastSeverity.INFO, state.activeToasts.first().severity)
    }

    @Test
    fun dismissRemovesToast() {
        val state = TabToastState()
        state.error("first")
        state.error("second")
        val idToRemove = state.activeToasts.first().id
        state.dismiss(idToRemove)
        assertEquals(1, state.activeToasts.size)
        assertEquals("second", state.activeToasts.first().message)
    }

    @Test
    fun dismissClearsElapsedTracking() {
        val state = TabToastState()
        state.info("tracked")
        val id = state.activeToasts.first().id
        state.recordElapsed(id, 2500L)
        assertEquals(2500L, state.elapsedVisibleMs(id))
        state.dismiss(id)
        assertEquals(0L, state.elapsedVisibleMs(id))
    }

    @Test
    fun elapsedVisibleMsDefaultsToZero() {
        val state = TabToastState()
        state.info("new toast")
        val id = state.activeToasts.first().id
        assertEquals(0L, state.elapsedVisibleMs(id))
    }

    @Test
    fun recordElapsedUpdatesTrackedTime() {
        val state = TabToastState()
        state.info("toast")
        val id = state.activeToasts.first().id
        state.recordElapsed(id, 1000L)
        assertEquals(1000L, state.elapsedVisibleMs(id))
        state.recordElapsed(id, 3000L)
        assertEquals(3000L, state.elapsedVisibleMs(id))
    }

    @Test
    fun recordElapsedIgnoresDismissedToast() {
        val state = TabToastState()
        state.info("gone")
        val id = state.activeToasts.first().id
        state.dismiss(id)
        state.recordElapsed(id, 5000L)
        assertEquals(0L, state.elapsedVisibleMs(id))
    }

    @Test
    fun multipleToastsAccumulateInOrder() {
        val state = TabToastState()
        state.info("first")
        state.warning("second")
        state.error("third")
        assertEquals(3, state.activeToasts.size)
        assertEquals(listOf("first", "second", "third"), state.activeToasts.map { it.message })
    }
}
