package com.strangeparticle.springboard.app.ui.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import com.strangeparticle.springboard.app.domain.factory.currentTimeMillis
import com.strangeparticle.springboard.app.platform.copyToClipboard
import com.strangeparticle.springboard.app.ui.TestTags
import com.strangeparticle.springboard.app.ui.brand.*
import com.strangeparticle.springboard.app.ui.brand.LocalUiBrand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ToastOverlay(
    tabToastState: TabToastState? = null,
    isTabVisible: Boolean = true,
    onToastDismissed: () -> Unit = {},
) {
    var globalToasts by remember { mutableStateOf(listOf<ToastMessage>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        ToastBroadcaster.toasts.collect { toast ->
            globalToasts = globalToasts + toast
            if (toast.severity == ToastSeverity.INFO) {
                scope.launch {
                    delay(CommonUiConstants.ToastAutoDismissMs)
                    globalToasts = globalToasts.filter { it.id != toast.id }
                    onToastDismissed()
                }
            }
        }
    }

    val tabToasts = if (isTabVisible) tabToastState?.activeToasts ?: emptyList() else emptyList()

    TabToastAutoDismiss(
        tabToastState = tabToastState,
        isTabVisible = isTabVisible,
        onToastDismissed = onToastDismissed,
    )

    val allToasts = tabToasts + globalToasts

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier.padding(16.dp).width(CommonUiConstants.ToastWidth),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allToasts.forEach { toast ->
                val isTabToast = tabToasts.any { it.id == toast.id }
                ToastCard(
                    toast = toast,
                    onDismiss = {
                        if (isTabToast) {
                            tabToastState?.dismiss(toast.id)
                        } else {
                            globalToasts = globalToasts.filter { it.id != toast.id }
                        }
                        onToastDismissed()
                    }
                )
            }
        }
    }
}

@Composable
private fun TabToastAutoDismiss(
    tabToastState: TabToastState?,
    isTabVisible: Boolean,
    onToastDismissed: () -> Unit,
) {
    if (tabToastState == null) return

    val infoToasts = tabToastState.activeToasts.filter { it.severity == ToastSeverity.INFO }

    for (toast in infoToasts) {
        key(toast.id) {
            val elapsed = tabToastState.elapsedVisibleMs(toast.id)
            val remaining = CommonUiConstants.ToastAutoDismissMs - elapsed

            if (remaining <= 0) {
                LaunchedEffect(Unit) {
                    tabToastState.dismiss(toast.id)
                    onToastDismissed()
                }
            } else if (isTabVisible) {
                DisposableEffect(toast.id) {
                    val startTime = currentTimeMillis()
                    onDispose {
                        val newElapsed = elapsed + (currentTimeMillis() - startTime)
                        tabToastState.recordElapsed(toast.id, newElapsed)
                    }
                }
                LaunchedEffect(toast.id) {
                    delay(remaining)
                    tabToastState.dismiss(toast.id)
                    onToastDismissed()
                }
            }
        }
    }
}

@Composable
internal fun ToastCard(toast: ToastMessage, onDismiss: () -> Unit) {
    var showCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val currentUiBrand = LocalUiBrand.current
    val (background, borderColor, textColor, icon, severityLabel) = when (toast.severity) {
        ToastSeverity.ERROR -> ToastStyle(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onErrorContainer,
            currentUiBrand.vectorImages.severityError,
            "Error"
        )
        ToastSeverity.WARNING -> ToastStyle(
            currentUiBrand.customColors.toastWarningBackground,
            currentUiBrand.customColors.toastWarningBorder,
            currentUiBrand.customColors.toastWarningText,
            currentUiBrand.vectorImages.severityWarning,
            "Warning"
        )
        ToastSeverity.INFO -> ToastStyle(
            currentUiBrand.customColors.toastInfoBackground,
            currentUiBrand.customColors.toastInfoBorder,
            currentUiBrand.customColors.toastInfoText,
            currentUiBrand.vectorImages.severityInfo,
            "Info"
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
                    imageVector = icon,
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
                        imageVector = if (showCopied) currentUiBrand.vectorImages.copyConfirmed else currentUiBrand.vectorImages.copy,
                        contentDescription = if (showCopied) "Copied" else "Copy",
                        tint = textColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp).testTag(TestTags.TOAST_DISMISS_BUTTON)) {
                    Icon(
                        imageVector = currentUiBrand.vectorImages.dismiss,
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

internal data class ToastStyle(
    val background: Color,
    val border: Color,
    val text: Color,
    val icon: ImageVector,
    val label: String
)
