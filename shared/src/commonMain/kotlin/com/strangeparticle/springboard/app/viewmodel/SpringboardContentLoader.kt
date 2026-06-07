package com.strangeparticle.springboard.app.viewmodel

/** Loads the raw JSON contents for a springboard source (file path or URL). */
interface SpringboardContentLoader {
    suspend fun loadContent(source: String): String
}
