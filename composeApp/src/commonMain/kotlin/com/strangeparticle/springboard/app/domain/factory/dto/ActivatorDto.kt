package com.strangeparticle.springboard.app.domain.factory.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
internal sealed class ActivatorDto {
    abstract val appId: String
    abstract val resourceId: String
    abstract val environmentId: String
}

@Serializable
@SerialName("url")
internal data class UrlActivatorDto(
    override val appId: String,
    override val resourceId: String,
    override val environmentId: String,
    val url: String
) : ActivatorDto()

@Serializable
@SerialName("urlTemplate")
internal data class UrlTemplateActivatorDto(
    override val appId: String,
    override val resourceId: String,
    override val environmentId: String,
    val urlTemplate: String
) : ActivatorDto()

@Serializable
@SerialName("cmd")
internal data class CommandActivatorDto(
    override val appId: String,
    override val resourceId: String,
    override val environmentId: String,
    val commandTemplate: String
) : ActivatorDto()
