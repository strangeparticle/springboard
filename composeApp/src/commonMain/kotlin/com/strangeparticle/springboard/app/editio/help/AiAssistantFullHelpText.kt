package com.strangeparticle.springboard.app.editio.help

internal object AiAssistantFullHelpText {
    const val title = "AI Editing Assistant help"

    val text: String = """
        $title

        Tell the assistant what you want changed in plain language. It can inspect and edit the current Springboard by using tools.

        Common requests:
        - Add or update apps, app groups, resources, and environments.
        - Add URL, URL-template, or command activators for environment/app/resource cells.
        - Add or update guidance notes for specific cells.
        - Open, create, close, or save tabs when the request is clear.

        Useful concepts:
        - Apps are the services or launchable applications this Springboard organizes.
        - Resources are per-service targets like dashboards, logs, repos, CI, or runbooks.
        - Environments represent contexts such as sandbox, staging, and prod. The reserved environment id "ALL" applies to every environment.
        - Activators connect an environment, app, and resource to a URL, URL template, or command action.
        - Guidance stores notes for a specific environment/app/resource coordinate.

        Not supported:
        - The assistant cannot activate, launch, or open URLs or commands from the grid. Activation is done directly through the Springboard grid UI.

        Slash commands:
        - /help shows this help locally. It is included in the visible transcript, but it is not sent to the AI provider.

        Tips:
        - Refer to existing things by their visible names when possible.
        - You can ask for multiple related edits in one message.
        - The assistant should ask a clarifying question only when the target or intended outcome is ambiguous.
        - Saving changes may require confirmation.
    """.trimIndent()
}
