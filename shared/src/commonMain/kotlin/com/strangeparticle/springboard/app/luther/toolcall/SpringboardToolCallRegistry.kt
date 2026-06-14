package com.strangeparticle.springboard.app.luther.toolcall

import com.strangeparticle.luther.core.toolcall.ToolCallHandler
import com.strangeparticle.luther.core.toolcall.ToolCallRegistry

internal fun createSpringboardToolCallRegistry(): ToolCallRegistry = ToolCallRegistry().apply {
    springboardToolCallHandlers().forEach { register(it) }
}

internal fun springboardToolCallHandlers(): List<ToolCallHandler> = buildList {
    add(ActivateColumnToolCallHandler())
        add(ActivateCoordinateToolCallHandler())
        add(ActivateCoordinatesToolCallHandler())
        add(ActivateRowToolCallHandler())
        add(AddAppGroupToolCallHandler())
        add(AddAppToolCallHandler())
        add(AddCommandActivatorToolCallHandler())
        add(AddEnvironmentToolCallHandler())
        add(AddGuidanceToolCallHandler())
        add(AddResourceToolCallHandler())
        add(AddTerminalActivatorToolCallHandler())
        add(AddUrlActivatorToolCallHandler())
        add(AddUrlTemplateActivatorToolCallHandler())
        add(ChangeAppGroupIdToolCallHandler())
        add(ChangeAppIdToolCallHandler())
        add(ChangeEnvironmentIdToolCallHandler())
        add(ChangeResourceIdToolCallHandler())
        add(ChangeResourceNameToolCallHandler())
        add(CloseTabToolCallHandler())
        add(CreateSpringboardToolCallHandler())
        add(CreateTabToolCallHandler())
        add(GetSnapshotToolCallHandler())
        add(MoveActivatorToolCallHandler())
        add(OpenFromUrlToolCallHandler())
        add(OpenLocalFileToolCallHandler())
        add(RedoToolCallHandler())
        add(RemoveActivatorToolCallHandler())
        add(RemoveAppGroupToolCallHandler())
        add(RemoveAppToolCallHandler())
        add(RemoveEnvironmentToolCallHandler())
        add(RemoveGuidanceToolCallHandler())
        add(RemoveResourceToolCallHandler())
        add(ReorderActivatorsToolCallHandler())
        add(ReorderAppGroupsToolCallHandler())
        add(ReorderAppsToolCallHandler())
        add(ReorderEnvironmentsToolCallHandler())
        add(ReorderResourcesToolCallHandler())
        add(RespondWithMessageToolCallHandler())
        add(SaveSpringboardToolCallHandler())
        add(UndoToolCallHandler())
        add(UpdateActivatorToolCallHandler())
        add(UpdateAppGroupToolCallHandler())
        add(UpdateAppToolCallHandler())
        add(UpdateEnvironmentToolCallHandler())
        add(UpdateGuidanceToolCallHandler())
}
