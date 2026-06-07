package com.strangeparticle.springboard.app.luther.help

internal object AiAssistantTerseHelpText {
    val text: String = """
        Describe the springboard change you desire. E.g.: "add a metrics URL for the fretnaut app in prod", "give the fretnaut app a github URL in all environments", etc. Reference apps/columns, app groups, resources/rows, environments (staging, prod; the reserved "ALL" applies to every env) and corresponding activators (url activator or command activator) with optional guidance lines.  You can also ask the assistant to trigger activators, e.g. "open the prod dashboard for fretnaut", "open all the confluence links".
    """.trimIndent()
}
