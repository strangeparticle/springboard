package com.strangeparticle.springboard.app.platform

class PlatformFileContentServiceDefaultImpl : PlatformFileContentService {

    // Fully qualified to disambiguate from the same-named override methods in this class.
    override fun readFileContents(path: String): String? =
        com.strangeparticle.springboard.app.platform.readFileContents(path)

    override fun writeFileContents(path: String, contents: String): Boolean =
        com.strangeparticle.springboard.app.platform.writeFileContents(path, contents)
}
