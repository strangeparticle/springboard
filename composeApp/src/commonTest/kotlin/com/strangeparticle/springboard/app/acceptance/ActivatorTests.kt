package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Test

class ActivatorTests {

    @Test fun `single URL activator opens URL in browser`() =
        ActivatorTestScenarios.singleUrlActivatorOpensUrlInBrowser()

    @Test fun `multiple URL activators each open in browser`() =
        ActivatorTestScenarios.multipleUrlActivatorsEachOpenInBrowser()
}
