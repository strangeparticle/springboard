package com.strangeparticle.springboard.app.ui.gridnav

const val MaxHeaderChars = 20

fun truncateHeaderText(text: String): String =
    if (text.length > MaxHeaderChars) text.take(MaxHeaderChars - 1) + "…" else text
