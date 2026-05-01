package com.strangeparticle.springboard.app.shared

import com.strangeparticle.springboard.app.platform.PlatformFileContentService
import com.strangeparticle.springboard.app.viewmodel.SpringboardContentLoader

/**
 * Test loader that resolves contents by delegating to a [PlatformFileContentService]
 * fake. Used so reload acceptance tests can mutate file contents through the file
 * fake and have those changes observable on reload, the same way they were before
 * reload was unified behind [SpringboardContentLoader].
 */
class SpringboardContentLoaderInMemoryFake(
    private val fileContentService: PlatformFileContentService,
) : SpringboardContentLoader {
    override suspend fun loadContent(source: String): String {
        return fileContentService.readFileContents(source)
            ?: throw IllegalStateException("File not found: $source")
    }
}
