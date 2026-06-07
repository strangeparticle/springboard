package com.strangeparticle.springboard.app.ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.strangeparticle.springboard.app.legal.loadBundledLicenseText
import com.strangeparticle.springboard.app.ui.toast.ToastBroadcaster

@Composable
fun LicenseDialog(
    onClose: () -> Unit,
) {
    val licenseText = remember {
        try {
            loadBundledLicenseText()
        } catch (exception: IllegalStateException) {
            ToastBroadcaster.error("License text is unavailable: ${exception.message}")
            "Licenses and notices are unavailable because the bundled legal resources could not be loaded."
        }
    }

    DialogWindow(
        onCloseRequest = onClose,
        title = "Licenses and Notices",
        resizable = true,
        state = rememberDialogState(width = 680.dp, height = 520.dp),
    ) {
        val scrollState = rememberScrollState()

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 240.dp)
                        .sizeIn(minWidth = 360.dp, minHeight = 240.dp),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(16.dp),
                        ) {
                            Text(
                                text = licenseText,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Button(onClick = onClose) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
