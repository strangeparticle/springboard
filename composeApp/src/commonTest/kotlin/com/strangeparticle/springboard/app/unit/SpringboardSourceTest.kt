package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.domain.SpringboardSource
import com.strangeparticle.springboard.app.domain.parseSpringboardSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SpringboardSourceTest {

    @Test fun `https URL returns HttpSource`() {
        val result = parseSpringboardSource("https://example.com/board.json")
        assertEquals(SpringboardSource.HttpSource("https://example.com/board.json"), result)
    }

    @Test fun `http URL returns HttpSource`() {
        val result = parseSpringboardSource("http://example.com/board.json")
        assertEquals(SpringboardSource.HttpSource("http://example.com/board.json"), result)
    }

    @Test fun `HTTPS uppercase returns HttpSource`() {
        val result = parseSpringboardSource("HTTPS://example.com/board.json")
        assertEquals(SpringboardSource.HttpSource("HTTPS://example.com/board.json"), result)
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

    @Test fun `s3 URL returns S3Source with bucket and key`() {
        val result = parseSpringboardSource("s3://my-bucket/board.json")
        assertEquals(SpringboardSource.S3Source("my-bucket", "board.json"), result)
    }

    @Test fun `s3 URL uppercase scheme returns S3Source`() {
        val result = parseSpringboardSource("S3://my-bucket/board.json")
        assertEquals(SpringboardSource.S3Source("my-bucket", "board.json"), result)
    }

    @Test fun `s3 URL with nested key returns S3Source with full key path`() {
        val result = parseSpringboardSource("s3://my-bucket/folder/sub/board.json")
        assertEquals(SpringboardSource.S3Source("my-bucket", "folder/sub/board.json"), result)
    }

    @Test fun `s3 URL preserves case of key`() {
        val result = parseSpringboardSource("s3://my-bucket/Folder/Board.JSON")
        assertEquals(SpringboardSource.S3Source("my-bucket", "Folder/Board.JSON"), result)
    }

    @Test fun `s3 URL with no key throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            parseSpringboardSource("s3://my-bucket")
        }
    }

    @Test fun `s3 URL with trailing slash and no key throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            parseSpringboardSource("s3://my-bucket/")
        }
    }

    @Test fun `s3 URL with empty bucket throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            parseSpringboardSource("s3:///board.json")
        }
    }
}
