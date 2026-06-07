package com.strangeparticle.springboard.app.platform

class PlatformBrowserDetectionServiceDefaultImpl : PlatformBrowserDetectionService {
    override fun detectDefaultBrowser(): DesktopBrowser =
        com.strangeparticle.springboard.app.platform.detectDefaultBrowser()
}
