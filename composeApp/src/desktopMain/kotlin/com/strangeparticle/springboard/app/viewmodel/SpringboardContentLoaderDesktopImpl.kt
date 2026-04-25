package com.strangeparticle.springboard.app.viewmodel

import com.strangeparticle.springboard.app.domain.SpringboardSource
import com.strangeparticle.springboard.app.domain.determineSpringboardSource
import com.strangeparticle.springboard.app.domain.toHttpsUrl
import com.strangeparticle.springboard.app.platform.NetworkContentService
import java.io.File

class SpringboardContentLoaderDesktopImpl(
    private val networkContentService: NetworkContentService,
) : SpringboardContentLoader {

    override suspend fun loadContent(source: String): String {
        return when (val springboardSource = determineSpringboardSource(source)) {
            is SpringboardSource.HttpSource -> networkContentService.fetchText(springboardSource.url)
            is SpringboardSource.S3Source -> networkContentService.fetchText(springboardSource.toHttpsUrl())
            is SpringboardSource.FileSource -> readLocalFile(springboardSource.path)
        }
    }

    private fun readLocalFile(path: String): String {
        val expanded = if (path.startsWith("~/")) {
            System.getProperty("user.home") + path.removePrefix("~")
        } else {
            path
        }
        val file = File(expanded)
        if (!file.exists()) {
            throw IllegalStateException("File not found: $expanded")
        }
        return file.readText()
    }
}
