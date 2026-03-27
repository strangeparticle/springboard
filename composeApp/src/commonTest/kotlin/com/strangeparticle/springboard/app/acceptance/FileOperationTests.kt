package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Ignore
import kotlin.test.Test

class FileOperationTests {

    @Ignore @Test fun `opening a valid springboard loads it successfully`() =
        FileOperationTestScenarios.openingAValidSpringboardLoadsItSuccessfully()

    @Ignore @Test fun `open shows file path and time - opening a springboard shows file path in status bar`() =
        FileOperationTestScenarios.openingASpringboardShowsFilePathInStatusBar()

    @Ignore @Test fun `open shows file path and time - opening a springboard shows open time in status bar`() =
        FileOperationTestScenarios.openingASpringboardShowsOpenTimeInStatusBar()

    @Ignore @Test fun `open error handling - opening an empty file shows error`() =
        FileOperationTestScenarios.openingAnEmptyFileShowsError()

    @Ignore @Test fun `open error handling - opening a file with malformed json shows error`() =
        FileOperationTestScenarios.openingAFileWithMalformedJsonShowsError()

    @Ignore @Test fun `open error handling - opening a file with invalid id shows error`() =
        FileOperationTestScenarios.openingAFileWithInvalidIdShowsError()

    @Ignore @Test fun `reloading reflects new file content`() =
        FileOperationTestScenarios.reloadingReflectsNewFileContent()

    @Ignore @Test fun `reloading updates the open time in status bar`() =
        FileOperationTestScenarios.reloadingUpdatesTheOpenTimeInStatusBar()

    @Ignore @Test fun `reload error handling - reloading an emptied file shows error`() =
        FileOperationTestScenarios.reloadingAnEmptiedFileShowsError()

    @Ignore @Test fun `reload error handling - reloading a file with malformed json shows error`() =
        FileOperationTestScenarios.reloadingAFileWithMalformedJsonShowsError()

    @Ignore @Test fun `reload error handling - reloading a file with invalid id shows error`() =
        FileOperationTestScenarios.reloadingAFileWithInvalidIdShowsError()

    @Ignore @Test fun `reload error handling - reloading a deleted file shows error`() =
        FileOperationTestScenarios.reloadingADeletedFileShowsError()

    @Ignore @Test fun `load embedded loads the built-in springboard`() =
        FileOperationTestScenarios.loadEmbeddedLoadsTheBuiltInSpringboard()

    @Ignore @Test fun `load embedded updates status line to embedded path`() =
        FileOperationTestScenarios.loadEmbeddedUpdatesStatusLineToEmbeddedPath()

    @Ignore @Test fun `load embedded updates the open time in status bar`() =
        FileOperationTestScenarios.loadEmbeddedUpdatesTheOpenTimeInStatusBar()

    @Ignore @Test fun `reload button shows correct icon`() =
        FileOperationTestScenarios.reloadButtonShowsCorrectIcon()

    @Ignore @Test fun `reload button spin behavior - reload button spins during reload`() =
        FileOperationTestScenarios.reloadButtonSpinsDuringReload()

    @Ignore @Test fun `reload button spin behavior - reload button spins for at least the minimum duration`() =
        FileOperationTestScenarios.reloadButtonSpinsForAtLeastTheMinimumDuration()

    @Ignore @Test fun `reload button spin behavior - reload button stops spinning after successful reload`() =
        FileOperationTestScenarios.reloadButtonStopsSpinningAfterSuccessfulReload()

    @Ignore @Test fun `reload button spin behavior - reload button stops spinning after failed reload`() =
        FileOperationTestScenarios.reloadButtonStopsSpinningAfterFailedReload()

    @Ignore @Test fun `save local copy as - save local copy as is disabled before any springboard is loaded`() =
        FileOperationTestScenarios.saveLocalCopyAsIsDisabledBeforeAnySpringboardIsLoaded()

    @Ignore @Test fun `save local copy as - save local copy as becomes enabled after a springboard is loaded`() =
        FileOperationTestScenarios.saveLocalCopyAsBecomesEnabledAfterASpringboardIsLoaded()

    @Ignore @Test fun `keyboard shortcut triggers reload`() =
        FileOperationTestScenarios.keyboardShortcutTriggersReload()
}
