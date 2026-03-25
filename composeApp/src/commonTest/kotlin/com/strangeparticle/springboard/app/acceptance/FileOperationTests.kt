package com.strangeparticle.springboard.app.acceptance

import kotlin.test.Ignore
import kotlin.test.Test

class FileOperationTests {

    class OpenValidSpringboard {
        @Ignore @Test fun `opening a valid springboard loads it successfully`() =
            FileOperationTestScenarios.openingAValidSpringboardLoadsItSuccessfully()
    }

    class OpenShowsFilePathAndTime {
        @Ignore @Test fun `opening a springboard shows file path in status bar`() =
            FileOperationTestScenarios.openingASpringboardShowsFilePathInStatusBar()

        @Ignore @Test fun `opening a springboard shows open time in status bar`() =
            FileOperationTestScenarios.openingASpringboardShowsOpenTimeInStatusBar()
    }

    class OpenErrorHandling {
        @Ignore @Test fun `opening an empty file shows error`() =
            FileOperationTestScenarios.openingAnEmptyFileShowsError()

        @Ignore @Test fun `opening a file with malformed json shows error`() =
            FileOperationTestScenarios.openingAFileWithMalformedJsonShowsError()

        @Ignore @Test fun `opening a file with invalid id shows error`() =
            FileOperationTestScenarios.openingAFileWithInvalidIdShowsError()
    }

    class ReloadUpdatesContent {
        @Ignore @Test fun `reloading reflects new file content`() =
            FileOperationTestScenarios.reloadingReflectsNewFileContent()
    }

    class ReloadUpdatesOpenTime {
        @Ignore @Test fun `reloading updates the open time in status bar`() =
            FileOperationTestScenarios.reloadingUpdatesTheOpenTimeInStatusBar()
    }

    class ReloadErrorHandling {
        @Ignore @Test fun `reloading an emptied file shows error`() =
            FileOperationTestScenarios.reloadingAnEmptiedFileShowsError()

        @Ignore @Test fun `reloading a file with malformed json shows error`() =
            FileOperationTestScenarios.reloadingAFileWithMalformedJsonShowsError()

        @Ignore @Test fun `reloading a file with invalid id shows error`() =
            FileOperationTestScenarios.reloadingAFileWithInvalidIdShowsError()

        @Ignore @Test fun `reloading a deleted file shows error`() =
            FileOperationTestScenarios.reloadingADeletedFileShowsError()
    }

    class LoadEmbeddedLoadsSpringboard {
        @Ignore @Test fun `load embedded loads the built-in springboard`() =
            FileOperationTestScenarios.loadEmbeddedLoadsTheBuiltInSpringboard()
    }

    class LoadEmbeddedUpdatesStatusLinePath {
        @Ignore @Test fun `load embedded updates status line to embedded path`() =
            FileOperationTestScenarios.loadEmbeddedUpdatesStatusLineToEmbeddedPath()
    }

    class LoadEmbeddedUpdatesStatusLineOpenTime {
        @Ignore @Test fun `load embedded updates the open time in status bar`() =
            FileOperationTestScenarios.loadEmbeddedUpdatesTheOpenTimeInStatusBar()
    }

    class ReloadButtonIcon {
        @Ignore @Test fun `reload button shows correct icon`() =
            FileOperationTestScenarios.reloadButtonShowsCorrectIcon()
    }

    class ReloadButtonSpinBehavior {
        @Ignore @Test fun `reload button spins during reload`() =
            FileOperationTestScenarios.reloadButtonSpinsDuringReload()

        @Ignore @Test fun `reload button spins for at least the minimum duration`() =
            FileOperationTestScenarios.reloadButtonSpinsForAtLeastTheMinimumDuration()

        @Ignore @Test fun `reload button stops spinning after successful reload`() =
            FileOperationTestScenarios.reloadButtonStopsSpinningAfterSuccessfulReload()

        @Ignore @Test fun `reload button stops spinning after failed reload`() =
            FileOperationTestScenarios.reloadButtonStopsSpinningAfterFailedReload()
    }

    class SaveLocalCopyAsDisabledWithoutActiveSpringboard {
        @Ignore @Test fun `save local copy as is disabled before any springboard is loaded`() =
            FileOperationTestScenarios.saveLocalCopyAsIsDisabledBeforeAnySpringboardIsLoaded()

        @Ignore @Test fun `save local copy as becomes enabled after a springboard is loaded`() =
            FileOperationTestScenarios.saveLocalCopyAsBecomesEnabledAfterASpringboardIsLoaded()
    }

    class KeyboardShortcutReload {
        @Ignore @Test fun `keyboard shortcut triggers reload`() =
            FileOperationTestScenarios.keyboardShortcutTriggersReload()
    }
}
