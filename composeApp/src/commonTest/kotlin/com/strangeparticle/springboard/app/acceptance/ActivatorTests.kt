package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Ignore
import kotlin.test.Test

class ActivatorTests {

    class UrlActivation {
        @Ignore @Test fun `single URL activator opens URL in browser`() =
            ActivatorTestScenarios.singleUrlActivatorOpensUrlInBrowser()
    }

    class UrlActivationMultiple {
        @Ignore @Test fun `multiple URL activators each open in browser`() =
            ActivatorTestScenarios.multipleUrlActivatorsEachOpenInBrowser()
    }
}
