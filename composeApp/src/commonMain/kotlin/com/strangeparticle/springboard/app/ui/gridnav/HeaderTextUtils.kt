package com.strangeparticle.springboard.app.ui.gridnav

import com.strangeparticle.springboard.app.domain.model.*

const val MaxHeaderChars = 20

fun truncateHeaderText(text: String): String =
    if (text.length > MaxHeaderChars) text.take(MaxHeaderChars - 1) + "…" else text

fun activatorPreviewText(activator: Activator?): String? = when (activator) {
    is UrlActivator -> "url: ${activator.url}"
    is UrlTemplateActivator -> "url: ${activator.urlTemplate}"
    is CommandActivator -> "cmd: ${activator.commandTemplate}"
    null -> null
}
