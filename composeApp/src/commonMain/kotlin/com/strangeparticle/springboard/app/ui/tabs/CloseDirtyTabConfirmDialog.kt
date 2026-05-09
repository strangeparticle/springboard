package com.strangeparticle.springboard.app.ui.tabs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.strangeparticle.springboard.app.ui.TestTags

/**
 * Confirmation dialog raised when the user attempts to close a tab whose
 * springboard has unsaved in-memory edits (dirty state).
 *
 * The dialog blocks the close until the user explicitly confirms or cancels.
 * "Close anyway" discards the in-memory edits; "Cancel" dismisses the dialog
 * and leaves the tab open. There is no "Save and close" path in MVP — the user
 * cancels, saves manually, then closes.
 */
@Composable
fun CloseDirtyTabConfirmDialog(
    tabLabel: String,
    onCancel: () -> Unit,
    onCloseAnyway: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Close without saving?") },
        text = {
            Text(
                "The tab \"$tabLabel\" has unsaved changes. Close anyway? " +
                    "Your changes will be discarded."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onCloseAnyway,
                modifier = Modifier.testTag(TestTags.CLOSE_DIRTY_TAB_CONFIRM_BUTTON),
            ) {
                Text("Close anyway")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag(TestTags.CLOSE_DIRTY_TAB_CANCEL_BUTTON),
            ) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag(TestTags.CLOSE_DIRTY_TAB_DIALOG),
    )
}
