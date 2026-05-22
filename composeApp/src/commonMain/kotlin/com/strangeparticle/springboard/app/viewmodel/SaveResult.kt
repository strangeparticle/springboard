package com.strangeparticle.springboard.app.viewmodel

/**
 * Outcome of a save operation initiated from [SpringboardViewModel.saveActiveTab] or
 * [SpringboardViewModel.saveActiveTabAs]. Callers are expected to dispatch the result
 * to user-facing feedback (e.g. toasts).
 */
sealed class SaveResult {

    /** The springboard was successfully written to [path]. */
    data class Success(val path: String) : SaveResult()

    /**
     * The write attempt reached [PlatformFileContentService.writeFileContents] and that
     * call returned `false` (or threw). [path] is where the write was attempted.
     */
    data class WriteFailed(val path: String, val errorMessage: String) : SaveResult()

    /**
     * The active tab's source is non-saveable in place (HTTP/HTTPS URL). The user
     * should be directed to Save As. The corresponding menu item should be disabled
     * and never invoke `saveActiveTab` in this state, so this result represents an
     * unexpected programmatic call rather than user error.
     */
    object NotSupportedForSource : SaveResult()

    /** No springboard is loaded in the active tab. */
    object NoSpringboard : SaveResult()
}
