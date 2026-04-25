package com.strangeparticle.springboard.app.viewmodel

import com.strangeparticle.springboard.app.domain.SpringboardSource
import com.strangeparticle.springboard.app.domain.determineSpringboardSource
import com.strangeparticle.springboard.app.domain.toHttpsUrl
import com.strangeparticle.springboard.app.platform.NetworkContentService

class SpringboardContentLoaderWasmImpl(
    private val networkContentService: NetworkContentService,
) : SpringboardContentLoader {

    override suspend fun loadContent(source: String): String {
        return when (val springboardSource = determineSpringboardSource(source)) {
            is SpringboardSource.HttpSource -> networkContentService.fetchText(springboardSource.url)
            is SpringboardSource.S3Source -> networkContentService.fetchText(springboardSource.toHttpsUrl())
            is SpringboardSource.FileSource ->
                throw IllegalStateException("WASM platform only supports s3 and http/https sources, got: $source")
        }
    }
}
