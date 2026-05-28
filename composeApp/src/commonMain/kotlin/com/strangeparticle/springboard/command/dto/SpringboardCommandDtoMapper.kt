package com.strangeparticle.springboard.command.dto

import com.strangeparticle.springboard.command.SpringboardCommand
import com.strangeparticle.springboard.command.SpringboardCommandArgument
import com.strangeparticle.springboard.command.SpringboardCommandArgumentType
import com.strangeparticle.springboard.command.SpringboardCommandDefinition
import com.strangeparticle.springboard.command.SpringboardCommandErrorCode
import com.strangeparticle.springboard.command.SpringboardCommandResult

object SpringboardCommandDtoMapper {
    fun toCommand(dto: SpringboardCommandDto): SpringboardCommand = when (dto) {
        SpringboardCommandDto.Status -> SpringboardCommand.Status
        is SpringboardCommandDto.ActivateCoordinate -> SpringboardCommand.ActivateCoordinate(
            tabId = dto.tabId,
            environmentId = dto.environmentId,
            appId = dto.appId,
            resourceId = dto.resourceId,
        )
        is SpringboardCommandDto.OpenSpringboard -> SpringboardCommand.OpenSpringboard(
            source = dto.source,
            inNewTab = dto.inNewTab,
        )
        is SpringboardCommandDto.SwitchTab -> SpringboardCommand.SwitchTab(
            tabIndex = dto.tabIndex,
        )
        is SpringboardCommandDto.ShowGuidance -> SpringboardCommand.ShowGuidance(
            environmentId = dto.environmentId,
            appId = dto.appId,
            resourceId = dto.resourceId,
        )
    }

    fun toCommand(dto: SpringboardActivateCoordinateRequestDto): SpringboardCommand.ActivateCoordinate =
        SpringboardCommand.ActivateCoordinate(
            tabId = dto.tabId,
            environmentId = dto.environmentId,
            appId = dto.appId,
            resourceId = dto.resourceId,
        )

    fun toCommand(dto: SpringboardOpenSpringboardRequestDto): SpringboardCommand.OpenSpringboard =
        SpringboardCommand.OpenSpringboard(
            source = dto.source,
            inNewTab = dto.inNewTab,
        )

    fun toCommand(dto: SpringboardSwitchTabRequestDto): SpringboardCommand.SwitchTab =
        SpringboardCommand.SwitchTab(tabIndex = dto.tabIndex)

    fun toCommand(dto: SpringboardShowGuidanceRequestDto): SpringboardCommand.ShowGuidance =
        SpringboardCommand.ShowGuidance(
            environmentId = dto.environmentId,
            appId = dto.appId,
            resourceId = dto.resourceId,
        )

    fun toCommandDto(dto: SpringboardActivateCoordinateRequestDto): SpringboardCommandDto.ActivateCoordinate =
        SpringboardCommandDto.ActivateCoordinate(
            tabId = dto.tabId,
            environmentId = dto.environmentId,
            appId = dto.appId,
            resourceId = dto.resourceId,
        )

    fun toCommandDto(dto: SpringboardOpenSpringboardRequestDto): SpringboardCommandDto.OpenSpringboard =
        SpringboardCommandDto.OpenSpringboard(
            source = dto.source,
            inNewTab = dto.inNewTab,
        )

    fun toCommandDto(dto: SpringboardSwitchTabRequestDto): SpringboardCommandDto.SwitchTab =
        SpringboardCommandDto.SwitchTab(tabIndex = dto.tabIndex)

    fun toCommandDto(dto: SpringboardShowGuidanceRequestDto): SpringboardCommandDto.ShowGuidance =
        SpringboardCommandDto.ShowGuidance(
            environmentId = dto.environmentId,
            appId = dto.appId,
            resourceId = dto.resourceId,
        )

    fun toCommandResult(dto: SpringboardCommandDto): SpringboardCommandResult {
        return when (dto) {
            SpringboardCommandDto.Status -> SpringboardCommandResult.Success()
            is SpringboardCommandDto.ActivateCoordinate -> validateRequiredStrings(
                "environmentId" to dto.environmentId,
                "appId" to dto.appId,
                "resourceId" to dto.resourceId,
            )
            is SpringboardCommandDto.OpenSpringboard -> validateRequiredStrings("source" to dto.source)
            is SpringboardCommandDto.SwitchTab -> {
                if (dto.tabIndex > 0) {
                    SpringboardCommandResult.Success()
                } else {
                    SpringboardCommandResult.Failure(
                        code = SpringboardCommandErrorCode.ValidationFailed,
                        message = "tabIndex must be greater than zero.",
                    )
                }
            }
            is SpringboardCommandDto.ShowGuidance -> validateRequiredStrings(
                "environmentId" to dto.environmentId,
                "appId" to dto.appId,
                "resourceId" to dto.resourceId,
            )
        }
    }

    fun toDto(command: SpringboardCommand): SpringboardCommandDto = when (command) {
        SpringboardCommand.Status -> SpringboardCommandDto.Status
        is SpringboardCommand.ActivateCoordinate -> SpringboardCommandDto.ActivateCoordinate(
            tabId = command.tabId,
            environmentId = command.environmentId,
            appId = command.appId,
            resourceId = command.resourceId,
        )
        is SpringboardCommand.OpenSpringboard -> SpringboardCommandDto.OpenSpringboard(
            source = command.source,
            inNewTab = command.inNewTab,
        )
        is SpringboardCommand.SwitchTab -> SpringboardCommandDto.SwitchTab(
            tabIndex = command.tabIndex,
        )
        is SpringboardCommand.ShowGuidance -> SpringboardCommandDto.ShowGuidance(
            environmentId = command.environmentId,
            appId = command.appId,
            resourceId = command.resourceId,
        )
    }

    fun toDto(result: SpringboardCommandResult): SpringboardCommandResultDto = when (result) {
        is SpringboardCommandResult.Success -> SpringboardCommandResultDto.Success(
            message = result.message,
            data = result.data,
        )
        is SpringboardCommandResult.Failure -> SpringboardCommandResultDto.Failure(
            code = result.code.wireValue,
            message = result.message,
            details = result.details,
        )
    }

    fun toResult(dto: SpringboardCommandResultDto): SpringboardCommandResult = when (dto) {
        is SpringboardCommandResultDto.Success -> SpringboardCommandResult.Success(
            message = dto.message,
            data = dto.data,
        )
        is SpringboardCommandResultDto.Failure -> SpringboardCommandResult.Failure(
            code = SpringboardCommandErrorCode.fromWireValue(dto.code),
            message = dto.message,
            details = dto.details,
        )
    }

    fun toDto(definition: SpringboardCommandDefinition): SpringboardCommandDefinitionDto =
        SpringboardCommandDefinitionDto(
            id = definition.id,
            toolCallName = definition.toolCallName,
            summary = definition.summary,
            arguments = definition.arguments.map(::toDto),
        )

    private fun toDto(argument: SpringboardCommandArgument): SpringboardCommandArgumentDto =
        SpringboardCommandArgumentDto(
            id = argument.id,
            toolCallName = argument.toolCallName,
            description = argument.description,
            required = argument.required,
            valueType = when (argument.valueType) {
                SpringboardCommandArgumentType.String -> "string"
                SpringboardCommandArgumentType.Int -> "int"
                SpringboardCommandArgumentType.Boolean -> "boolean"
            },
        )

    private fun validateRequiredStrings(vararg arguments: Pair<String, String>): SpringboardCommandResult {
        val blankArgument = arguments.firstOrNull { (_, value) -> value.isBlank() }
        return if (blankArgument == null) {
            SpringboardCommandResult.Success()
        } else {
            SpringboardCommandResult.Failure(
                code = SpringboardCommandErrorCode.ValidationFailed,
                message = "${blankArgument.first} must not be blank.",
            )
        }
    }
}
