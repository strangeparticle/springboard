package com.strangeparticle.springboard.app.platform

import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

class NetworkContentServiceWasmImpl : NetworkContentService {

    private val client = HttpClient(Js)

    override suspend fun fetchText(url: String): String {
        val response = client.get(url)
        if (response.status.value !in 200..299) {
            throw Exception("HTTP ${response.status.value}: ${response.status.description}")
        }
        return response.bodyAsText()
    }
}
