package com.strangeparticle.springboard.app.viewmodel

import com.strangeparticle.springboard.app.platform.NetworkContentService
import java.io.File

class SpringboardContentLoaderDesktopImpl(
    private val networkContentService: NetworkContentService,
) : SpringboardContentLoader {

    override suspend fun loadContent(source: String): String {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            return networkContentService.fetchText(source)
        }
        val raw = if (source.startsWith("file://")) source.removePrefix("file://") else source
        val path = if (raw.startsWith("~/")) System.getProperty("user.home") + raw.removePrefix("~") else raw
        val file = File(path)
        if (!file.exists()) {
            throw IllegalStateException("File not found: $path")
        }
        return file.readText()
    }
}
