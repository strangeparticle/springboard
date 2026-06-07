package com.strangeparticle.springboard.app.command

interface CommandApiServer {
    fun start(): CommandApiServerHandle
}

interface CommandApiServerHandle {
    val baseUrl: String
    val token: String
    fun stop()
}
