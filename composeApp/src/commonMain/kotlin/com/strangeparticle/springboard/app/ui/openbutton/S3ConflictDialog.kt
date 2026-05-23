package com.strangeparticle.springboard.app.ui.openbutton

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Shown after a save returns [com.strangeparticle.springboard.app.viewmodel.SaveResult.Conflict]
 * — the S3 object's ETag no longer matches the one captured at load time. Lets
 * the user pick a resolution.
 */
@Composable
fun S3ConflictDialog(
    sourceUrl: String,
    onOverwrite: () -> Unit,
    onReload: () -> Unit,
    onSaveAs: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.6f),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    "Modified externally",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "The object at $sourceUrl has changed since this tab was loaded. " +
                        "Overwrite remote changes, reload and discard local edits, or save a local copy?",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = onSaveAs,
                        modifier = Modifier.height(32.dp).defaultMinSize(minWidth = 92.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) { Text("Save As…", fontSize = 13.sp) }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = onReload,
                        modifier = Modifier.height(32.dp).defaultMinSize(minWidth = 92.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) { Text("Reload", fontSize = 13.sp) }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = onOverwrite,
                        modifier = Modifier.height(32.dp).defaultMinSize(minWidth = 92.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    ) { Text("Overwrite", fontSize = 13.sp) }
                }
            }
        }
    }
}
