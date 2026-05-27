package com.strangeparticle.springboard.app.unit.tools

import com.strangeparticle.springboard.app.luther.toolcall.SaveSpringboardToolCallHandler
import com.strangeparticle.springboard.app.luther.toolcall.SaveSpringboardToolCallHandlerRequest
import com.strangeparticle.springboard.app.persistence.PersistenceServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformActivationServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.PlatformFileContentServiceInMemoryFake
import com.strangeparticle.springboard.app.shared.TestFixtureJson
import com.strangeparticle.springboard.app.shared.SpringboardToolCallExecutionContextInMemoryFake
import com.strangeparticle.springboard.app.shared.createSettingsManagerForTest
import com.strangeparticle.springboard.app.viewmodel.SpringboardViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class SaveSpringboardToolCallHandlerTest {

    private fun loadedContext(
        source: String = "/test.json",
        json: String = TestFixtureJson.URL_ONLY,
    ): Triple<SpringboardToolCallExecutionContextInMemoryFake, String, PlatformFileContentServiceInMemoryFake> {
        val fileService = PlatformFileContentServiceInMemoryFake()
        val vm = SpringboardViewModel(
            settingsManager = createSettingsManagerForTest(),
            persistenceService = PersistenceServiceInMemoryFake(),
            platformActivationService = PlatformActivationServiceInMemoryFake(),
            fileContentService = fileService,
        )
        vm.loadConfig(json, source)
        vm.markActiveTabDirty()
        return Triple(SpringboardToolCallExecutionContextInMemoryFake(viewModel = vm), vm.activeTabId, fileService)
    }

    @Test
    fun `save approved by user writes the springboard and reports success`() = runTest {
        val (ctx, tabId, fileService) = loadedContext(source = "/path/test.json")

        val executeJob = async {
            SaveSpringboardToolCallHandler().executeToolCallHandler(
                toolCallId = "call-1",
                args = SaveSpringboardToolCallHandlerRequest(tab_id = tabId, display_message = "x"),
                context = ctx,
            )
        }
        ctx.resolveApproval("call-1", approved = true)
        val result = executeJob.await()

        assertTrue(result.success)
        assertTrue(fileService.writtenFiles.containsKey("/path/test.json"))
        assertFalse(ctx.viewModel.activeTab!!.isDirty)
    }

    @Test
    fun `save denied by user returns user_declined error and does not write`() = runTest {
        val (ctx, tabId, fileService) = loadedContext()

        val executeJob = async {
            SaveSpringboardToolCallHandler().executeToolCallHandler(
                toolCallId = "call-2",
                args = SaveSpringboardToolCallHandlerRequest(tab_id = tabId, display_message = "x"),
                context = ctx,
            )
        }
        ctx.resolveApproval("call-2", approved = false)
        val result = executeJob.await()

        assertFalse(result.success)
        assertEquals("user_declined", result.code)
        assertTrue(fileService.writtenFiles.isEmpty(), "no write should happen on denial")
        assertTrue(ctx.viewModel.activeTab!!.isDirty, "dirty stays set on denial")
    }

    @Test
    fun `save reports write_failed when fileService returns false`() = runTest {
        val (ctx, tabId, fileService) = loadedContext()
        fileService.writeReturnsOverride = false

        val executeJob = async {
            SaveSpringboardToolCallHandler().executeToolCallHandler(
                toolCallId = "call-3",
                args = SaveSpringboardToolCallHandlerRequest(tab_id = tabId, display_message = "x"),
                context = ctx,
            )
        }
        ctx.resolveApproval("call-3", approved = true)
        val result = executeJob.await()

        assertFalse(result.success)
        assertEquals("write_failed", result.code)
        assertTrue(ctx.viewModel.activeTab!!.isDirty)
    }

    @Test
    fun `save refuses for HTTP source without prompting for confirmation`() = runTest {
        val (ctx, tabId, _) = loadedContext(source = "https://example.com/sb.json")

        val result = SaveSpringboardToolCallHandler().executeToolCallHandler(
            toolCallId = "call-4",
            args = SaveSpringboardToolCallHandlerRequest(tab_id = tabId, display_message = "x"),
            context = ctx,
        )

        assertFalse(result.success)
        assertEquals("not_supported_for_source", result.code)
        assertTrue(ctx.pendingApprovals.isEmpty())
    }

    @Test
    fun `save refuses for HTTPS source`() = runTest {
        val (ctx, tabId, _) = loadedContext(source = "https://bucket.s3.us-gov-west-1.amazonaws.com/key.json")

        val result = SaveSpringboardToolCallHandler().executeToolCallHandler(
            toolCallId = "call-5",
            args = SaveSpringboardToolCallHandlerRequest(tab_id = tabId, display_message = "x"),
            context = ctx,
        )

        assertFalse(result.success)
        assertEquals("not_supported_for_source", result.code)
    }

    @Test
    fun `save refuses for missing tab`() = runTest {
        val (ctx, _, _) = loadedContext()

        val result = SaveSpringboardToolCallHandler().executeToolCallHandler(
            toolCallId = "call-6",
            args = SaveSpringboardToolCallHandlerRequest(tab_id = "no_such", display_message = "x"),
            context = ctx,
        )

        assertFalse(result.success)
        assertEquals("missing_tab", result.code)
    }

    @Test
    fun `save refuses for empty tab without springboard`() = runTest {
        val vm = SpringboardViewModel(
            settingsManager = createSettingsManagerForTest(),
            persistenceService = PersistenceServiceInMemoryFake(),
            platformActivationService = PlatformActivationServiceInMemoryFake(),
        )
        val ctx = SpringboardToolCallExecutionContextInMemoryFake(viewModel = vm)

        val result = SaveSpringboardToolCallHandler().executeToolCallHandler(
            toolCallId = "call-7",
            args = SaveSpringboardToolCallHandlerRequest(tab_id = vm.activeTabId, display_message = "x"),
            context = ctx,
        )

        assertFalse(result.success)
        assertEquals("tab_empty", result.code)
    }

    @Test
    fun `save refuses for source-less springboard without prompting for confirmation`() = runTest {
        val vm = SpringboardViewModel(
            settingsManager = createSettingsManagerForTest(),
            persistenceService = PersistenceServiceInMemoryFake(),
            platformActivationService = PlatformActivationServiceInMemoryFake(),
        )
        vm.createUnsavedSpringboardTab()
        val tabId = vm.activeTabId
        val ctx = SpringboardToolCallExecutionContextInMemoryFake(viewModel = vm)

        val result = SaveSpringboardToolCallHandler().executeToolCallHandler(
            toolCallId = "call-sourceless",
            args = SaveSpringboardToolCallHandlerRequest(tab_id = tabId, display_message = "x"),
            context = ctx,
        )

        assertFalse(result.success)
        assertEquals("not_supported_for_source", result.code)
        assertTrue(ctx.pendingApprovals.isEmpty())
    }

    @Test
    fun `save targeting non-active tab writes file without changing active tab`() = runTest {
        val fileService = PlatformFileContentServiceInMemoryFake()
        val vm = SpringboardViewModel(
            settingsManager = createSettingsManagerForTest(),
            persistenceService = PersistenceServiceInMemoryFake(),
            platformActivationService = PlatformActivationServiceInMemoryFake(),
            fileContentService = fileService,
        )
        val targetTabId = vm.activeTabId
        vm.loadConfig(TestFixtureJson.URL_ONLY, "/target.json")
        vm.markActiveTabDirty()
        vm.createTab()
        val activeTabId = vm.activeTabId
        assertNotEquals(targetTabId, activeTabId, "target and active tabs must differ for this test")

        val ctx = SpringboardToolCallExecutionContextInMemoryFake(viewModel = vm)
        val executeJob = async {
            SaveSpringboardToolCallHandler().executeToolCallHandler(
                toolCallId = "call-8",
                args = SaveSpringboardToolCallHandlerRequest(tab_id = targetTabId, display_message = "x"),
                context = ctx,
            )
        }
        ctx.resolveApproval("call-8", approved = true)
        val result = executeJob.await()

        assertTrue(result.success)
        assertTrue(fileService.writtenFiles.containsKey("/target.json"))
        assertFalse(vm.findTab(targetTabId)!!.isDirty, "target tab dirty should be cleared")
        assertEquals(activeTabId, vm.activeTabId, "active tab must not change")
    }
}
