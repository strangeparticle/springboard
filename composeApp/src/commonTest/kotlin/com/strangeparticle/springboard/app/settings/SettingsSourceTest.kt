package com.strangeparticle.springboard.app.settings

import kotlin.test.*

class SettingsSourceTest {

    @Test
    fun testPrecedenceChainIsNotEmpty() {
        assertTrue(PRECEDENCE_CHAIN.isNotEmpty(), "Precedence chain must not be empty")
    }

    @Test
    fun testPrecedenceChainContainsNoDuplicates() {
        val distinct = PRECEDENCE_CHAIN.distinct()
        assertEquals(
            PRECEDENCE_CHAIN.size, distinct.size,
            "Precedence chain must not contain duplicate entries"
        )
    }

    @Test
    fun testPrecedenceChainContainsAllSources() {
        for (source in SettingsSource.entries) {
            assertTrue(
                source in PRECEDENCE_CHAIN,
                "Precedence chain must contain $source"
            )
        }
    }
}
