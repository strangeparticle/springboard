package com.strangeparticle.springboard.app.domain.model

const val WILDCARD_ENVIRONMENT_ID = "*"

sealed class Activator {
    abstract val appId: String
    abstract val resourceId: String
    abstract val environmentId: String

    abstract fun withEnvironmentId(newEnvironmentId: String): Activator
}

data class UrlActivator(
    override val appId: String,
    override val resourceId: String,
    override val environmentId: String,
    val url: String
) : Activator() {
    override fun withEnvironmentId(newEnvironmentId: String) = copy(environmentId = newEnvironmentId)
}

data class UrlTemplateActivator(
    override val appId: String,
    override val resourceId: String,
    override val environmentId: String,
    val urlTemplate: String
) : Activator() {
    override fun withEnvironmentId(newEnvironmentId: String) = copy(environmentId = newEnvironmentId)
}

data class CommandActivator(
    override val appId: String,
    override val resourceId: String,
    override val environmentId: String,
    val commandTemplate: String
) : Activator() {
    override fun withEnvironmentId(newEnvironmentId: String) = copy(environmentId = newEnvironmentId)
}
