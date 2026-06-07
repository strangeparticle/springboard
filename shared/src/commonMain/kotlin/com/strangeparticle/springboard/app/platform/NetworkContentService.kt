package com.strangeparticle.springboard.app.platform

interface NetworkContentService {
    suspend fun fetchText(url: String): String
}
