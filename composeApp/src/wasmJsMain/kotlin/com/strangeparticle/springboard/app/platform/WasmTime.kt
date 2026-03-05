package com.strangeparticle.springboard.app.domain.factory

import kotlinx.browser.window

internal actual fun currentTimeMillis(): Long =
    window.performance.now().toLong()
