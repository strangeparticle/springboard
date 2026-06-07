package com.strangeparticle.springboard.app.ui.gridnav

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

@Composable
fun GridNavGuidanceCornerIndicator(
    color: Color,
    creaseColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val triangle = Path().apply {
            moveTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, 0f)
            close()
        }

        drawPath(path = triangle, color = color)
        drawLine(
            color = creaseColor,
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
        )
    }
}
