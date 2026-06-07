package com.strangeparticle.springboard.app.platform

interface PlatformBrowserDetectionService {
    fun detectDefaultBrowser(): DesktopBrowser
}
