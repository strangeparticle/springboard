package com.strangeparticle.springboard.command

data class SpringboardCommandArgument(
    val id: String,
    val toolCallName: String,
    val description: String,
    val required: Boolean,
    val valueType: SpringboardCommandArgumentType,
)

enum class SpringboardCommandArgumentType {
    String,
    Int,
    Boolean,
}
