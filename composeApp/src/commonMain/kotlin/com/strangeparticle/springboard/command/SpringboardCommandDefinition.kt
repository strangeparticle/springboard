package com.strangeparticle.springboard.command

data class SpringboardCommandDefinition(
    val id: String,
    val toolCallName: String,
    val summary: String,
    val arguments: List<SpringboardCommandArgument>,
)
