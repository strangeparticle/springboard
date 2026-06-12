package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.strangeparticle.springboard.app.ui.TestTags

/**
 * Confirmation dialog raised before completing a row/column header group activation.
 *
 * Clicking a row or column header activates every item in that row/column at once,
 * which can open many URLs or run many commands from a single accidental click. This
 * dialog blocks the group activation until the user explicitly confirms. "Activate"
 * runs the group; "Cancel" dismisses the dialog and runs nothing. The dialog always
 * shows for header group activation — there is no count threshold.
 */
@Composable
fun GroupActivationConfirmDialog(
    itemCount: Int,
    onCancel: () -> Unit,
    onActivate: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Activate $itemCount items?") },
        text = {
            Text(
                "This will activate all $itemCount items in the selected row or column."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onActivate,
                modifier = Modifier.testTag(TestTags.GROUP_ACTIVATION_CONFIRM_BUTTON),
            ) {
                Text("Activate")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag(TestTags.GROUP_ACTIVATION_CANCEL_BUTTON),
            ) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag(TestTags.GROUP_ACTIVATION_CONFIRM_DIALOG),
    )
}
