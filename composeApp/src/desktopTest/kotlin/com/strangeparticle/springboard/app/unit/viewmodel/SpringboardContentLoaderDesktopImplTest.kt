package com.strangeparticle.springboard.app.unit.viewmodel

import com.strangeparticle.springboard.app.platform.NetworkContentService
import com.strangeparticle.springboard.app.viewmodel.SpringboardContentLoaderDesktopImpl
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SpringboardContentLoaderDesktopImplTest {

    private class FakeNetworkContentService(
        private val textByUrl: Map<String, String>,
    ) : NetworkContentService {
        val calls = mutableListOf<String>()
        override suspend fun fetchText(url: String): String {
            calls += url
            return textByUrl[url] ?: throw IllegalStateException("no mapping for: $url")
        }
    }

    @Test
    fun `http URL routes to network service`() = runTest {
        val network = FakeNetworkContentService(
            mapOf("http://example.com/board.json" to "network content")
        )
        val loader = SpringboardContentLoaderDesktopImpl(network)

        val result = loader.loadContent("http://example.com/board.json")

        assertEquals("network content", result)
        assertEquals(listOf("http://example.com/board.json"), network.calls)
    }

    @Test
    fun `https URL routes to network service`() = runTest {
        val network = FakeNetworkContentService(
            mapOf("https://example.com/board.json" to "secure content")
        )
        val loader = SpringboardContentLoaderDesktopImpl(network)

        val result = loader.loadContent("https://example.com/board.json")

        assertEquals("secure content", result)
        assertEquals(listOf("https://example.com/board.json"), network.calls)
    }

    @Test
    fun `file URL with triple slash reads from filesystem`() = runTest {
        val tempFile = File.createTempFile("loader-test", ".json")
        tempFile.writeText("file content")
        tempFile.deleteOnExit()
        val network = FakeNetworkContentService(emptyMap())
        val loader = SpringboardContentLoaderDesktopImpl(network)

        val result = loader.loadContent("file://${tempFile.absolutePath}")

        assertEquals("file content", result)
        assertTrue(network.calls.isEmpty())
    }

    @Test
    fun `bare path reads from filesystem`() = runTest {
        val tempFile = File.createTempFile("loader-test", ".json")
        tempFile.writeText("bare content")
        tempFile.deleteOnExit()
        val network = FakeNetworkContentService(emptyMap())
        val loader = SpringboardContentLoaderDesktopImpl(network)

        val result = loader.loadContent(tempFile.absolutePath)

        assertEquals("bare content", result)
        assertTrue(network.calls.isEmpty())
    }

    @Test
    fun `missing file throws IllegalStateException`() = runTest {
        val network = FakeNetworkContentService(emptyMap())
        val loader = SpringboardContentLoaderDesktopImpl(network)

        val thrown = assertFailsWith<IllegalStateException> {
            loader.loadContent("/nonexistent/path/to/file.json")
        }
        assertTrue(thrown.message!!.contains("/nonexistent/path/to/file.json"))
    }
}
