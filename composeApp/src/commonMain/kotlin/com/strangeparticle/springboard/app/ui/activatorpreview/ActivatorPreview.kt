package com.strangeparticle.springboard.app.ui.activatorpreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.strangeparticle.springboard.app.ui.theme.color.ActivatorPreviewText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ActivatorPreview(previewText: String?) {
    if (previewText != null) {
        Text(
            text = previewText,
            fontSize = 11.sp,
            color = ActivatorPreviewText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
