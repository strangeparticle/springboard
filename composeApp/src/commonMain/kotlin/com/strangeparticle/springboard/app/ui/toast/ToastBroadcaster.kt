package com.strangeparticle.springboard.app.ui.toast

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class ToastSeverity { ERROR, WARNING, INFO }

data class ToastMessage(
    val id: Long = nextId(),
    val message: String,
    val severity: ToastSeverity
) {
    companion object {
        private var counter = 0L
        private fun nextId(): Long = ++counter
    }
}

object ToastBroadcaster {
    private val _toasts = MutableSharedFlow<ToastMessage>(extraBufferCapacity = 16)
    val toasts = _toasts.asSharedFlow()

    fun error(message: String) {
        _toasts.tryEmit(ToastMessage(message = message, severity = ToastSeverity.ERROR))
    }

    fun warning(message: String) {
        _toasts.tryEmit(ToastMessage(message = message, severity = ToastSeverity.WARNING))
    }

    fun info(message: String) {
        _toasts.tryEmit(ToastMessage(message = message, severity = ToastSeverity.INFO))
    }
}
