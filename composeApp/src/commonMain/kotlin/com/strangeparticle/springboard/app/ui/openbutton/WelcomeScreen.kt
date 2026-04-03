package com.strangeparticle.springboard.app.ui.openbutton

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeparticle.springboard.app.platform.openFileDialog

@Composable
fun WelcomeScreen(
    onFileSelected: (String) -> Unit,
    onOpenFromNetwork: (() -> Unit)? = null,
    showFileOpen: Boolean = true,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showFileOpen) {
                Button(
                    onClick = {
                        val path = openFileDialog(null)
                        if (path != null) {
                            onFileSelected(path)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        "Open Springboard from File...",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 15.sp
                    )
                }
            }
            if (onOpenFromNetwork != null) {
                Button(
                    onClick = onOpenFromNetwork,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        "Open Springboard from Network...",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
