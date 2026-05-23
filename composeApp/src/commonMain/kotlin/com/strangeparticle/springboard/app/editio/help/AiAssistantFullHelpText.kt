package com.strangeparticle.springboard.app.editio.help

internal object AiAssistantFullHelpText {
    const val title = "AI Assistant help"

    val text: String = """
        $title

        Tell the assistant what you want changed in plain language. It can inspect and edit the current Springboard by using tools.

        Common requests:
        - Add or update apps, app groups, resources, and environments.
        - Add URL, URL-template, or command activators for environment/app/resource cells.
        - Add or update guidance notes for specific cells.
        - Open, create, close, or save tabs when the request is clear.
        - Open one or many entries — e.g. "open the prod dashboard for fretnaut", "open all the confluence links", "open everything for fretnaut in prod".

        Useful concepts:
        - Apps are the services or launchable applications this Springboard organizes.
        - Resources are per-service targets like dashboards, logs, repos, CI, or runbooks.
        - Environments represent contexts such as sandbox, staging, and prod. The reserved environment id "ALL" applies to every environment.
        - Activators connect an environment, app, and resource to a URL, URL template, or command action.
        - Guidance stores notes for a specific environment/app/resource coordinate.

        Slash commands:
        - /help shows this help locally. It is included in the visible transcript, but it is not sent to the AI provider.

        Tips:
        - Refer to existing things by their visible names when possible.
        - You can ask for multiple related edits in one message.
        - The assistant should ask a clarifying question only when the target or intended outcome is ambiguous.
        - Saving changes may require confirmation.
    """.trimIndent()
}
