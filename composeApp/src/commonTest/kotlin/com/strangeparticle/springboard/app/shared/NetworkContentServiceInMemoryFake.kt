package com.strangeparticle.springboard.app.shared

import com.strangeparticle.springboard.app.platform.NetworkContentService

class NetworkContentServiceInMemoryFake(
    private val responses: Map<String, String> = emptyMap(),
) : NetworkContentService {

    val fetchedUrls = mutableListOf<String>()

    override suspend fun fetchText(url: String): String {
        fetchedUrls.add(url)
        return responses[url] ?: throw Exception("No response configured for $url")
    }
}
