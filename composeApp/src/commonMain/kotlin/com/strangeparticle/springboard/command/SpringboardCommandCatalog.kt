package com.strangeparticle.springboard.command

object SpringboardCommandCatalog {
    val definitions: List<SpringboardCommandDefinition> = listOf(
        SpringboardCommandDefinition(
            id = "status",
            toolCallName = "status",
            summary = "Report whether the running Springboard app is reachable.",
            arguments = emptyList(),
        ),
        SpringboardCommandDefinition(
            id = "activateCoordinate",
            toolCallName = "activate_coordinate",
            summary = "Activate one Springboard coordinate by environment, app, and resource id.",
            arguments = listOf(
                SpringboardCommandArgument(
                    id = "environmentId",
                    toolCallName = "environment_id",
                    description = "Environment id for the coordinate. Use the reserved id ALL for all-environments activators.",
                    required = true,
                    valueType = SpringboardCommandArgumentType.String,
                ),
                SpringboardCommandArgument(
                    id = "appId",
                    toolCallName = "app_id",
                    description = "App id for the coordinate.",
                    required = true,
                    valueType = SpringboardCommandArgumentType.String,
                ),
                SpringboardCommandArgument(
                    id = "resourceId",
                    toolCallName = "resource_id",
                    description = "Resource id for the coordinate.",
                    required = true,
                    valueType = SpringboardCommandArgumentType.String,
                ),
            ),
        ),
        SpringboardCommandDefinition(
            id = "openSpringboard",
            toolCallName = "open_springboard",
            summary = "Open a Springboard source in the current tab or a new tab.",
            arguments = listOf(
                SpringboardCommandArgument(
                    id = "source",
                    toolCallName = "source",
                    description = "Local path or supported URL for the Springboard JSON source.",
                    required = true,
                    valueType = SpringboardCommandArgumentType.String,
                ),
                SpringboardCommandArgument(
                    id = "inNewTab",
                    toolCallName = "in_new_tab",
                    description = "Open the source in a new tab when true.",
                    required = false,
                    valueType = SpringboardCommandArgumentType.Boolean,
                ),
            ),
        ),
        SpringboardCommandDefinition(
            id = "switchTab",
            toolCallName = "switch_tab",
            summary = "Switch to an open tab by one-based tab index.",
            arguments = listOf(
                SpringboardCommandArgument(
                    id = "tabIndex",
                    toolCallName = "tab_index",
                    description = "One-based tab index to activate.",
                    required = true,
                    valueType = SpringboardCommandArgumentType.Int,
                ),
            ),
        ),
        SpringboardCommandDefinition(
            id = "showGuidance",
            toolCallName = "show_guidance",
            summary = "Select a coordinate so its guidance is shown in the running app.",
            arguments = listOf(
                SpringboardCommandArgument(
                    id = "environmentId",
                    toolCallName = "environment_id",
                    description = "Environment id for the coordinate. Use the reserved id ALL for all-environments guidance.",
                    required = true,
                    valueType = SpringboardCommandArgumentType.String,
                ),
                SpringboardCommandArgument(
                    id = "appId",
                    toolCallName = "app_id",
                    description = "App id for the coordinate.",
                    required = true,
                    valueType = SpringboardCommandArgumentType.String,
                ),
                SpringboardCommandArgument(
                    id = "resourceId",
                    toolCallName = "resource_id",
                    description = "Resource id for the coordinate.",
                    required = true,
                    valueType = SpringboardCommandArgumentType.String,
                ),
            ),
        ),
    )

    fun requireById(id: String): SpringboardCommandDefinition =
        definitions.firstOrNull { it.id == id } ?: error("Unknown Springboard command definition: $id")
}
