package com.strangeparticle.springboard.app.ui.toast

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TabToastState {
    var activeToasts by mutableStateOf(listOf<ToastMessage>())
        private set

    private val _elapsedVisibleMs = mutableMapOf<Long, Long>()

    fun error(message: String) {
        add(ToastMessage(message = message, severity = ToastSeverity.ERROR))
    }

    fun warning(message: String) {
        add(ToastMessage(message = message, severity = ToastSeverity.WARNING))
    }

    fun info(message: String) {
        add(ToastMessage(message = message, severity = ToastSeverity.INFO))
    }

    fun dismiss(id: Long) {
        activeToasts = activeToasts.filter { it.id != id }
        _elapsedVisibleMs.remove(id)
    }

    fun elapsedVisibleMs(id: Long): Long = _elapsedVisibleMs[id] ?: 0L

    fun recordElapsed(id: Long, elapsedMs: Long) {
        if (activeToasts.any { it.id == id }) {
            _elapsedVisibleMs[id] = elapsedMs
        }
    }

    private fun add(toast: ToastMessage) {
        activeToasts = activeToasts + toast
        _elapsedVisibleMs[toast.id] = 0L
    }
}
