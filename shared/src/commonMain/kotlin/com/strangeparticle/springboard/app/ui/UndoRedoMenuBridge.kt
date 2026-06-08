package com.strangeparticle.springboard.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Hoists the assistant-layer undo/redo availability and actions up to the desktop Edit menu so the
// menu can stay in sync without owning chat state.
class UndoRedoMenuBridge {
    var canUndo by mutableStateOf(false)
    var canRedo by mutableStateOf(false)
    var onUndo: () -> Unit = {}
    var onRedo: () -> Unit = {}
}
