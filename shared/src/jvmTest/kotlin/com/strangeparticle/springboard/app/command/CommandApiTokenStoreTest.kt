package com.strangeparticle.springboard.app.command

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class CommandApiTokenStoreTest {
    @Test
    fun `generates and persists on first call then reuses`() {
        val dir = Files.createTempDirectory("springboard-token-test")
        val store = CommandApiTokenStore(dir.resolve("command-api-token"))

        val first = store.resolveToken()
        assertTrue(first.length >= 32)
        val second = store.resolveToken()
        assertEquals(first, second)
    }

    @Test
    fun `rotate forces a new token`() {
        val dir = Files.createTempDirectory("springboard-token-test")
        val store = CommandApiTokenStore(dir.resolve("command-api-token"))
        val first = store.resolveToken()
        val rotated = store.resolveToken(rotate = true)
        assertNotEquals(first, rotated)
    }
}
