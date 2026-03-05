package com.strangeparticle.springboard.app.domain.factory.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class EnvironmentDto(val id: String, val name: String)
