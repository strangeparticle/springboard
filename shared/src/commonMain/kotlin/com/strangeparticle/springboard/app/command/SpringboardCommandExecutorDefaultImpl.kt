package com.strangeparticle.springboard.app.command

import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.domain.model.ALL_ENVS_ENVIRONMENT_ID
import com.strangeparticle.springboard.app.viewmodel.AssistantActivationOutcome
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel.LoadResult
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import com.strangeparticle.springboard.command.SpringboardCommand
import com.strangeparticle.springboard.command.SpringboardCommandErrorCode
import com.strangeparticle.springboard.command.SpringboardCommandResult
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SpringboardCommandExecutorDefaultImpl(
    private val viewModel: SpringboardViewModel,
) : SpringboardCommandExecutor {
    override suspend fun execute(command: SpringboardCommand): SpringboardCommandResult {
        return when (command) {
            SpringboardCommand.Status -> status()
            is SpringboardCommand.ActivateCoordinate -> activateCoordinate(command)
            is SpringboardCommand.OpenSpringboard -> openSpringboard(command)
            is SpringboardCommand.SwitchTab -> switchTab(command)
            is SpringboardCommand.ShowGuidance -> showGuidance(command)
        }
    }

    private fun status(): SpringboardCommandResult.Success {
        val activeTab = viewModel.activeTab
        return SpringboardCommandResult.Success(
            message = "Springboard is running.",
            data = buildJsonObject {
                put("running", true)
                put("activeTabId", viewModel.activeTabId)
                put("activeTabLabel", activeTab?.label)
                put("activeTabSource", activeTab?.source)
                put("tabCount", viewModel.tabs.size)
                put("loadedTabCount", viewModel.tabs.count { !it.isEmpty })
            },
        )
    }

    private fun activateCoordinate(command: SpringboardCommand.ActivateCoordinate): SpringboardCommandResult {
        val tabId = command.tabId ?: viewModel.activeTabId
        val outcome = viewModel.activateCoordinateFromAssistant(
            tabId = tabId,
            coordinate = Coordinate(
                environmentId = command.environmentId,
                appId = command.appId,
                resourceId = command.resourceId,
            ),
        )
        return outcome.toCommandResult(tabId)
    }

    private suspend fun openSpringboard(command: SpringboardCommand.OpenSpringboard): SpringboardCommandResult {
        return when (val result = viewModel.loadConfigFromSource(command.source, inNewTab = command.inNewTab)) {
            is LoadResult.Success -> {
                val label = viewModel.tabs.firstOrNull { it.tabId == result.tabId }?.label ?: command.source
                SpringboardCommandResult.Success(message = "Opened $label.")
            }
            is LoadResult.Failure -> SpringboardCommandResult.Failure(
                code = SpringboardCommandErrorCode.InternalError,
                message = result.message,
            )
        }
    }

    private fun switchTab(command: SpringboardCommand.SwitchTab): SpringboardCommandResult {
        val tab = viewModel.tabs.getOrNull(command.tabIndex - 1)
            ?: return SpringboardCommandResult.Failure(
                code = SpringboardCommandErrorCode.TabNotFound,
                message = "No tab found at index ${command.tabIndex}.",
            )

        viewModel.selectTab(tab.tabId)
        return SpringboardCommandResult.Success(message = "Switched to tab ${command.tabIndex}: ${tab.label}.")
    }

    private fun showGuidance(command: SpringboardCommand.ShowGuidance): SpringboardCommandResult {
        val springboard = viewModel.springboardFilteredForRuntime
            ?: return SpringboardCommandResult.Failure(
                code = SpringboardCommandErrorCode.SpringboardNotLoaded,
                message = "No springboard is loaded in the active tab.",
            )
        val coordinate = Coordinate(command.environmentId, command.appId, command.resourceId)
        if (!springboard.indexes.guidanceByCoordinate.containsKey(coordinate)) {
            return SpringboardCommandResult.Failure(
                code = SpringboardCommandErrorCode.CoordinateNotFound,
                message = "No guidance found for the requested coordinate.",
            )
        }

        viewModel.selectEnvironment(command.environmentId.takeUnless { it == ALL_ENVS_ENVIRONMENT_ID })
        viewModel.selectApp(command.appId)
        viewModel.selectResource(command.resourceId)
        return SpringboardCommandResult.Success(message = "Showing guidance for ${command.environmentId}/${command.appId}/${command.resourceId}.")
    }

    private fun AssistantActivationOutcome.toCommandResult(tabId: String): SpringboardCommandResult {
        return when (this) {
            is AssistantActivationOutcome.Success -> SpringboardCommandResult.Success(
                message = "Activated $activatorCount activator${if (activatorCount == 1) "" else "s"}.",
            )
            AssistantActivationOutcome.MissingTab -> SpringboardCommandResult.Failure(
                code = SpringboardCommandErrorCode.TabNotFound,
                message = "No tab found for id $tabId.",
            )
            AssistantActivationOutcome.TabEmpty -> SpringboardCommandResult.Failure(
                code = SpringboardCommandErrorCode.SpringboardNotLoaded,
                message = "No springboard is loaded in tab $tabId.",
            )
            AssistantActivationOutcome.NoActivatorsResolved -> SpringboardCommandResult.Failure(
                code = SpringboardCommandErrorCode.CoordinateNotFound,
                message = "No activator found for the requested coordinate.",
            )
        }
    }
}
