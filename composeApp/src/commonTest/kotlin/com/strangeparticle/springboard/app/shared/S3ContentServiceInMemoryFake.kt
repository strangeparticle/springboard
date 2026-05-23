package com.strangeparticle.springboard.app.shared

import com.strangeparticle.springboard.app.platform.S3ContentService
import com.strangeparticle.springboard.app.platform.S3GetResult
import com.strangeparticle.springboard.app.platform.S3PutResult

/**
 * Test double for [S3ContentService]. Backed by `(url, profile) -> Stored`
 * maps so tests can simulate read/write outcomes, including ETag-based
 * conflict scenarios.
 */
class S3ContentServiceInMemoryFake : S3ContentService {

    data class Stored(val content: String, val etag: String?)

    val objects: MutableMap<Key, Stored> = mutableMapOf()
    val getOverrides: MutableMap<Key, S3GetResult> = mutableMapOf()
    val putOverrides: MutableMap<Key, S3PutResult> = mutableMapOf()
    val putCalls: MutableList<PutCall> = mutableListOf()
    val getCalls: MutableList<Key> = mutableListOf()

    var nextEtag: String = "etag-1"

    data class Key(val url: String, val profile: String)
    data class PutCall(val url: String, val profile: String, val content: String, val ifMatch: String?)

    override suspend fun getObject(url: String, profile: String): S3GetResult {
        val key = Key(url, profile)
        getCalls += key
        getOverrides[key]?.let { return it }
        val stored = objects[key] ?: return S3GetResult.Failed("Object not found")
        return S3GetResult.Success(stored.content, stored.etag)
    }

    override suspend fun putObject(url: String, profile: String, content: String, ifMatch: String?): S3PutResult {
        val key = Key(url, profile)
        putCalls += PutCall(url, profile, content, ifMatch)
        putOverrides[key]?.let { return it }
        val stored = objects[key]
        if (ifMatch != null && stored != null && stored.etag != ifMatch) {
            return S3PutResult.Conflict("ETag mismatch")
        }
        val newEtag = nextEtag
        nextEtag = "${nextEtag}+"
        objects[key] = Stored(content, newEtag)
        return S3PutResult.Success(newEtag)
    }
}
