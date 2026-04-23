package com.strangeparticle.springboard.app.unit

import com.strangeparticle.springboard.app.domain.SpringboardSource
import com.strangeparticle.springboard.app.domain.toHttpsUrl
import kotlin.test.Test
import kotlin.test.assertEquals

class S3UrlTest {

    @Test fun `toHttpsUrl produces virtual-hosted style URL`() {
        val source = SpringboardSource.S3Source("my-bucket", "board.json")
        assertEquals("https://my-bucket.s3.amazonaws.com/board.json", source.toHttpsUrl())
    }

    @Test fun `toHttpsUrl preserves nested key paths`() {
        val source = SpringboardSource.S3Source("my-bucket", "folder/sub/board.json")
        assertEquals(
            "https://my-bucket.s3.amazonaws.com/folder/sub/board.json",
            source.toHttpsUrl(),
        )
    }

    @Test fun `toHttpsUrl preserves case of key`() {
        val source = SpringboardSource.S3Source("my-bucket", "Folder/Board.JSON")
        assertEquals(
            "https://my-bucket.s3.amazonaws.com/Folder/Board.JSON",
            source.toHttpsUrl(),
        )
    }
}
