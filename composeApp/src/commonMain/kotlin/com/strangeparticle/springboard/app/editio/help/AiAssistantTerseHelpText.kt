package com.strangeparticle.springboard.app.editio.help

internal object AiAssistantTerseHelpText {
    val text: String = """
        Describe the springboard change you desire. E.g.: "add a metrics URL for fretnaut in prod", "give fretnaut a github URL in all environments", etc. Reference apps (the services this springboard organizes) and app groups, resources (per-service targets like dashboards, logs, CI), environments (sandbox, staging, prod; the reserved "ALL" applies to every env) and corresponding activators (url activator or command activator).  Guidance lines can also be added to each activator.
    """.trimIndent()
}
