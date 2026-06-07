package com.strangeparticle.springboard.app.unit.tools

import com.strangeparticle.springboard.app.command.SpringboardCommandExecutor
import com.strangeparticle.springboard.app.luther.toolcall.ActivateCoordinateToolCallHandler
import com.strangeparticle.springboard.app.luther.toolcall.ActivateCoordinateToolCallHandlerRequest
import com.strangeparticle.springboard.app.shared.SpringboardToolCallExecutionContextInMemoryFake
import com.strangeparticle.springboard.app.shared.createViewModelForTest
import com.strangeparticle.springboard.command.SpringboardCommand
import com.strangeparticle.springboard.command.SpringboardCommandResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ActivateCoordinateToolCommandExecutorTest {
    @Test
    fun `activate coordinate tool routes through shared command executor`() = runTest {
        val executor = RecordingCommandExecutor()
        val context = SpringboardToolCallExecutionContextInMemoryFake(
            viewModel = createViewModelForTest(),
            commandExecutor = executor,
        )

        val response = ActivateCoordinateToolCallHandler().executeToolCallHandler(
            args = ActivateCoordinateToolCallHandlerRequest(
                tab_id = "tab-1",
                environment_id = "prod",
                app_id = "github",
                resource_id = "repo",
            ),
            context = context,
        )

        assertEquals(
            SpringboardCommand.ActivateCoordinate(
                tabId = "tab-1",
                environmentId = "prod",
                appId = "github",
                resourceId = "repo",
            ),
            executor.commands.single(),
        )
        assertEquals(true, response.success)
        assertEquals("Activated from fake executor.", response.message)
    }

    private class RecordingCommandExecutor : SpringboardCommandExecutor {
        val commands = mutableListOf<SpringboardCommand>()

        override suspend fun execute(command: SpringboardCommand): SpringboardCommandResult {
            commands += command
            return SpringboardCommandResult.Success("Activated from fake executor.")
        }
    }
}
