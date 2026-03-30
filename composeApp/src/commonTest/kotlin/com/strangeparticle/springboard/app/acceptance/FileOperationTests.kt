package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Test

class FileOperationTests {

    @Test fun `opening a valid springboard loads it successfully`() =
        FileOperationTestScenarios.openingAValidSpringboardLoadsItSuccessfully()

    @Test fun `open shows file path and time - opening a springboard shows file path in status bar`() =
        FileOperationTestScenarios.openingASpringboardShowsFilePathInStatusBar()

    @Test fun `open shows file path and time - opening a springboard shows open time in status bar`() =
        FileOperationTestScenarios.openingASpringboardShowsOpenTimeInStatusBar()

    @Test fun `open error handling - opening an empty file shows error`() =
        FileOperationTestScenarios.openingAnEmptyFileShowsError()

    @Test fun `open error handling - opening a file with malformed json shows error`() =
        FileOperationTestScenarios.openingAFileWithMalformedJsonShowsError()

    @Test fun `open error handling - opening a file with invalid id shows error`() =
        FileOperationTestScenarios.openingAFileWithInvalidIdShowsError()

    @Test fun `reloading reflects new file content`() =
        FileOperationTestScenarios.reloadingReflectsNewFileContent()

    @Test fun `reloading updates the open time in status bar`() =
        FileOperationTestScenarios.reloadingUpdatesTheOpenTimeInStatusBar()

    @Test fun `reload error handling - reloading an emptied file shows error`() =
        FileOperationTestScenarios.reloadingAnEmptiedFileShowsError()

    @Test fun `reload error handling - reloading a file with malformed json shows error`() =
        FileOperationTestScenarios.reloadingAFileWithMalformedJsonShowsError()

    @Test fun `reload error handling - reloading a file with invalid id shows error`() =
        FileOperationTestScenarios.reloadingAFileWithInvalidIdShowsError()

    @Test fun `reload error handling - reloading a deleted file shows error`() =
        FileOperationTestScenarios.reloadingADeletedFileShowsError()

    @Test fun `load embedded loads the built-in springboard`() =
        FileOperationTestScenarios.loadEmbeddedLoadsTheBuiltInSpringboard()

    @Test fun `load embedded updates status line to embedded path`() =
        FileOperationTestScenarios.loadEmbeddedUpdatesStatusLineToEmbeddedPath()

    @Test fun `load embedded updates the open time in status bar`() =
        FileOperationTestScenarios.loadEmbeddedUpdatesTheOpenTimeInStatusBar()

    @Test fun `reload button shows correct icon`() =
        FileOperationTestScenarios.reloadButtonShowsCorrectIcon()

    @Test fun `reload button spin behavior - reload button spins during reload`() =
        FileOperationTestScenarios.reloadButtonSpinsDuringReload()

    @Test fun `reload button spin behavior - reload button spins for at least the minimum duration`() =
        FileOperationTestScenarios.reloadButtonSpinsForAtLeastTheMinimumDuration()

    @Test fun `reload button spin behavior - reload button stops spinning after successful reload`() =
        FileOperationTestScenarios.reloadButtonStopsSpinningAfterSuccessfulReload()

    @Test fun `reload button spin behavior - reload button stops spinning after failed reload`() =
        FileOperationTestScenarios.reloadButtonStopsSpinningAfterFailedReload()

    // SaveLocalCopyAs disabled/enabled and KeyboardShortcutReload are manual-only for now:
    // SpringboardMenuBar is a FrameWindowScope extension whose items live outside the compose
    // tree and are not accessible from runComposeUiTest.
}
