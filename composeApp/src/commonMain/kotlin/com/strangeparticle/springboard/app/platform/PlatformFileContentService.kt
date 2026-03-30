package com.strangeparticle.springboard.app.platform

interface PlatformFileContentService {
    fun readFileContents(path: String): String?
    fun writeFileContents(path: String, contents: String): Boolean
}
