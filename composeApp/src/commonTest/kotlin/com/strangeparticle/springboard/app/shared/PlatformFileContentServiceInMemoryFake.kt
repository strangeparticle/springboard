package com.strangeparticle.springboard.app.shared

import com.strangeparticle.springboard.app.platform.PlatformFileContentService

class PlatformFileContentServiceInMemoryFake : PlatformFileContentService {
    val fileContents: MutableMap<String, String?> = mutableMapOf()

    // Writes are tracked separately so tests can assert what was written without
    // polluting the read map. Read-after-write on the same path returns null unless
    // the test explicitly seeds fileContents.
    val writtenFiles: MutableMap<String, String> = mutableMapOf()

    override fun readFileContents(path: String): String? = fileContents[path]

    override fun writeFileContents(path: String, contents: String): Boolean {
        writtenFiles[path] = contents
        return true
    }
}
