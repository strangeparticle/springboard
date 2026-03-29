package com.strangeparticle.springboard.app.ui.openbutton

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
fun OpenSpringboardPrompt(onFileSelected: (String) -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
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
                "Open Springboard...",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 15.sp
            )
        }
    }
}
