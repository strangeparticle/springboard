package com.strangeparticle.springboard.app.shared

import com.strangeparticle.springboard.app.platform.PlatformFileContentService

class PlatformFileContentServiceInMemoryFake : PlatformFileContentService {
    val fileContents: MutableMap<String, String?> = mutableMapOf()

    // Writes are tracked separately so tests can assert what was written without
    // polluting the read map. Read-after-write on the same path returns null unless
    // the test explicitly seeds fileContents.
    val writtenFiles: MutableMap<String, String> = mutableMapOf()

    /**
     * When non-null, [writeFileContents] uses this as the return value (or throws,
     * via [writeException]) instead of recording the write. Tests use this to
     * exercise the WriteFailed branch in callers like SpringboardViewModel.saveActiveTab.
     */
    var writeReturnsOverride: Boolean? = null
    var writeException: Throwable? = null

    override fun readFileContents(path: String): String? = fileContents[path]

    override fun writeFileContents(path: String, contents: String): Boolean {
        writeException?.let { throw it }
        val override = writeReturnsOverride
        if (override != null) return override
        writtenFiles[path] = contents
        return true
    }
}
