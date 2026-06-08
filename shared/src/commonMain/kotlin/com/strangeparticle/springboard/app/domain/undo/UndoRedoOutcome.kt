package com.strangeparticle.springboard.app.domain.undo

/**
 * Result of a viewmodel undo/redo action, indicating what happened so the caller can show a user-facing message.
 */
sealed class UndoRedoOutcome {
    data object Undone : UndoRedoOutcome()
    data object Redone : UndoRedoOutcome()
    data object NothingToUndo : UndoRedoOutcome()
    data object NothingToRedo : UndoRedoOutcome()
}
