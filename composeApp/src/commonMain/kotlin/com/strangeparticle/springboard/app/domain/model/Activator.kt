package com.strangeparticle.springboard.app.domain.model

sealed class Activator {
    abstract val appId: String
    abstract val resourceId: String
    abstract val environmentId: String
}

data class UrlActivator(
    override val appId: String,
    override val resourceId: String,
    override val environmentId: String,
    val url: String
) : Activator()

data class UrlTemplateActivator(
    override val appId: String,
    override val resourceId: String,
    override val environmentId: String,
    val urlTemplate: String
) : Activator()

data class CommandActivator(
    override val appId: String,
    override val resourceId: String,
    override val environmentId: String,
    val commandTemplate: String
) : Activator()
