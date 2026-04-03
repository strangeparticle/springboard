package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.domain.SpringboardSource
import com.strangeparticle.springboard.app.domain.parseSpringboardSource
import kotlin.test.Test
import kotlin.test.assertEquals

class SpringboardSourceTest {

    @Test fun `https URL returns NetworkSource`() {
        val result = parseSpringboardSource("https://example.com/board.json")
        assertEquals(SpringboardSource.NetworkSource("https://example.com/board.json"), result)
    }

    @Test fun `http URL returns NetworkSource`() {
        val result = parseSpringboardSource("http://example.com/board.json")
        assertEquals(SpringboardSource.NetworkSource("http://example.com/board.json"), result)
    }

    @Test fun `HTTPS uppercase returns NetworkSource`() {
        val result = parseSpringboardSource("HTTPS://example.com/board.json")
        assertEquals(SpringboardSource.NetworkSource("HTTPS://example.com/board.json"), result)
    }

    @Test fun `file triple-slash URL returns FileSource with path`() {
        val result = parseSpringboardSource("file:///Users/grey/board.json")
        assertEquals(SpringboardSource.FileSource("/Users/grey/board.json"), result)
    }

    @Test fun `file double-slash URL returns FileSource with path`() {
        val result = parseSpringboardSource("file://Users/grey/board.json")
        assertEquals(SpringboardSource.FileSource("Users/grey/board.json"), result)
    }

    @Test fun `bare path returns FileSource`() {
        val result = parseSpringboardSource("/Users/grey/board.json")
        assertEquals(SpringboardSource.FileSource("/Users/grey/board.json"), result)
    }

    @Test fun `path with tilde returns FileSource`() {
        val result = parseSpringboardSource("~/board.json")
        assertEquals(SpringboardSource.FileSource("~/board.json"), result)
    }

    @Test fun `relative path returns FileSource`() {
        val result = parseSpringboardSource("configs/board.json")
        assertEquals(SpringboardSource.FileSource("configs/board.json"), result)
    }
}
