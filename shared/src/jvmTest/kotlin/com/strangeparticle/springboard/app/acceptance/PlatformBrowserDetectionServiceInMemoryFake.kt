package com.strangeparticle.springboard.app.acceptance

import com.strangeparticle.springboard.app.platform.DesktopBrowser
import com.strangeparticle.springboard.app.platform.PlatformBrowserDetectionService

class PlatformBrowserDetectionServiceInMemoryFake(
    var browser: DesktopBrowser = DesktopBrowser.Safari,
) : PlatformBrowserDetectionService {
    override fun detectDefaultBrowser(): DesktopBrowser = browser
}
