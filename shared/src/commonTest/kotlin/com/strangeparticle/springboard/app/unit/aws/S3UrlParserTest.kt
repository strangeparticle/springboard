package com.strangeparticle.springboard.app.unit.aws

import com.strangeparticle.springboard.app.aws.S3UrlParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class S3UrlParserTest {

    @Test
    fun `accepts virtual-hosted url with explicit region`() {
        assertTrue(S3UrlParser.isValidVirtualHostedS3Url("https://my-bucket.s3.us-east-1.amazonaws.com/file.json"))
        assertTrue(S3UrlParser.isValidVirtualHostedS3Url("https://my-bucket.s3.eu-west-3.amazonaws.com/path/to/file.json"))
        assertTrue(S3UrlParser.isValidVirtualHostedS3Url("https://bucket-name-123.s3.ap-southeast-2.amazonaws.com/k"))
    }

    @Test
    fun `extracts region from virtual-hosted url`() {
        assertEquals("us-east-1", S3UrlParser.region("https://b.s3.us-east-1.amazonaws.com/k"))
        assertEquals("eu-west-3", S3UrlParser.region("https://b.s3.eu-west-3.amazonaws.com/k"))
        assertEquals("ap-southeast-2", S3UrlParser.region("https://b.s3.ap-southeast-2.amazonaws.com/x/y/z.json"))
    }

    @Test
    fun `trims surrounding whitespace before matching`() {
        assertTrue(S3UrlParser.isValidVirtualHostedS3Url("  https://b.s3.us-east-1.amazonaws.com/k  "))
        assertEquals("us-east-1", S3UrlParser.region("  https://b.s3.us-east-1.amazonaws.com/k  "))
    }

    @Test
    fun `rejects path-style url`() {
        assertFalse(S3UrlParser.isValidVirtualHostedS3Url("https://s3.us-east-1.amazonaws.com/bucket/key"))
        assertNull(S3UrlParser.region("https://s3.us-east-1.amazonaws.com/bucket/key"))
    }

    @Test
    fun `rejects legacy region-less form`() {
        assertFalse(S3UrlParser.isValidVirtualHostedS3Url("https://b.s3.amazonaws.com/key"))
        assertNull(S3UrlParser.region("https://b.s3.amazonaws.com/key"))
    }

    @Test
    fun `rejects s3 scheme`() {
        assertFalse(S3UrlParser.isValidVirtualHostedS3Url("s3://b/key"))
        assertNull(S3UrlParser.region("s3://b/key"))
    }

    @Test
    fun `rejects non-s3 hosts`() {
        assertFalse(S3UrlParser.isValidVirtualHostedS3Url("https://example.com/file.json"))
        assertFalse(S3UrlParser.isValidVirtualHostedS3Url("https://my-bucket.s3.us-east-1.example.com/key"))
    }

    @Test
    fun `rejects url with empty key`() {
        assertFalse(S3UrlParser.isValidVirtualHostedS3Url("https://b.s3.us-east-1.amazonaws.com/"))
    }
}
