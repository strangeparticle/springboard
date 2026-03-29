package com.strangeparticle.springboard.app.ui.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.selection.SelectionContainer
import com.strangeparticle.springboard.app.platform.copyToClipboard
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.theme.*
import com.strangeparticle.springboard.app.ui.theme.color.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ToastOverlay(onToastDismissed: () -> Unit = {}) {
    var activeToasts by remember { mutableStateOf(listOf<ToastMessage>()) }
    val scope = rememberCoroutineScope()

    // Collect incoming toasts and auto-dismiss INFO severity toasts after a timeout
    LaunchedEffect(Unit) {
        ToastBroadcaster.toasts.collect { toast ->
            activeToasts = activeToasts + toast
            if (toast.severity == ToastSeverity.INFO) {
                scope.launch {
                    delay(CommonUiConstants.ToastAutoDismissMs)
                    activeToasts = activeToasts.filter { it.id != toast.id }
                    onToastDismissed()
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier.padding(16.dp).width(CommonUiConstants.ToastWidth),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            activeToasts.forEach { toast ->
                ToastCard(
                    toast = toast,
                    onDismiss = {
                        activeToasts = activeToasts.filter { it.id != toast.id }
                        onToastDismissed()
                    }
                )
            }
        }
    }
}

@Composable
private fun ToastCard(toast: ToastMessage, onDismiss: () -> Unit) {
    var showCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val (background, borderColor, textColor, icon, severityLabel) = when (toast.severity) {
        ToastSeverity.ERROR -> ToastStyle(
            ToastErrorBackground, ToastErrorBorder, ToastErrorText,
            Icons.Outlined.Warning, "Error"
        )
        ToastSeverity.WARNING -> ToastStyle(
            ToastWarningBackground, ToastWarningBorder, ToastWarningText,
            Icons.Outlined.Info, "Warning"
        )
        ToastSeverity.INFO -> ToastStyle(
            ToastInfoBackground, ToastInfoBorder, ToastInfoText,
            Icons.Outlined.Info, "Info"
        )
    }

    val shape = RoundedCornerShape(8.dp)

    Surface(
        shape = shape,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(background, shape)
                .border(1.dp, borderColor, shape)
                .padding(12.dp)
        ) {
            // Header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = severityLabel,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).testTag(TestTags.TOAST_SEVERITY_LABEL)
                )
                IconButton(
                    onClick = {
                        copyToClipboard(toast.message)
                        showCopied = true
                        scope.launch {
                            delay(500)
                            showCopied = false
                        }
                    },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        if (showCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = if (showCopied) "Copied" else "Copy",
                        tint = textColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Body
            SelectionContainer {
                Text(
                    text = toast.message,
                    color = textColor,
                    fontSize = 13.sp,
                    modifier = Modifier.testTag(TestTags.TOAST_MESSAGE)
                )
            }
        }
    }
}

private data class ToastStyle(
    val background: Color,
    val border: Color,
    val text: Color,
    val icon: ImageVector,
    val label: String
)
