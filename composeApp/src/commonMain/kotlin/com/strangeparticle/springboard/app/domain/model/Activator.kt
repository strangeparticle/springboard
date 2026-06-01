package com.strangeparticle.springboard.app.domain.model

const val ALL_ENVS_ENVIRONMENT_ID = "ALL"

sealed class Activator {
    abstract val appId: String
    abstract val resourceId: String
    abstract val environmentId: String

    abstract fun withAppId(newAppId: String): Activator
    abstract fun withResourceId(newResourceId: String): Activator
    abstract fun withEnvironmentId(newEnvironmentId: String): Activator
}

data class UrlActivator(
    override val appId: String,
    override val resourceId: String,
    override val environmentId: String,
    val url: String
) : Activator() {
    override fun withAppId(newAppId: String) = copy(appId = newAppId)
    override fun withResourceId(newResourceId: String) = copy(resourceId = newResourceId)
    override fun withEnvironmentId(newEnvironmentId: String) = copy(environmentId = newEnvironmentId)
}

data class UrlTemplateActivator(
    override val appId: String,
    override val resourceId: String,
    override val environmentId: String,
    val urlTemplate: String
) : Activator() {
    override fun withAppId(newAppId: String) = copy(appId = newAppId)
    override fun withResourceId(newResourceId: String) = copy(resourceId = newResourceId)
    override fun withEnvironmentId(newEnvironmentId: String) = copy(environmentId = newEnvironmentId)
}

data class CommandActivator(
    override val appId: String,
    override val resourceId: String,
    override val environmentId: String,
    val commandTemplate: String
) : Activator() {
    override fun withAppId(newAppId: String) = copy(appId = newAppId)
    override fun withResourceId(newResourceId: String) = copy(resourceId = newResourceId)
    override fun withEnvironmentId(newEnvironmentId: String) = copy(environmentId = newEnvironmentId)
}

data class TerminalActivator(
    override val appId: String,
    override val resourceId: String,
    override val environmentId: String,
    val workingDirectory: String,
    val command: String?
) : Activator() {
    override fun withAppId(newAppId: String) = copy(appId = newAppId)
    override fun withResourceId(newResourceId: String) = copy(resourceId = newResourceId)
    override fun withEnvironmentId(newEnvironmentId: String) = copy(environmentId = newEnvironmentId)
}
