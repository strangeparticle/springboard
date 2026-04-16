package com.strangeparticle.springboard.app.viewmodel

import com.strangeparticle.springboard.app.platform.NetworkContentService

class SpringboardContentLoaderWasmImpl(
    private val networkContentService: NetworkContentService,
) : SpringboardContentLoader {

    override suspend fun loadContent(source: String): String {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            return networkContentService.fetchText(source)
        }
        throw IllegalStateException("WASM platform only supports http/https sources, got: $source")
    }
}
