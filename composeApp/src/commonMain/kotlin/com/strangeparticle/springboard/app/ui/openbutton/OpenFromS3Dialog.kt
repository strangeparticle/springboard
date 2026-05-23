package com.strangeparticle.springboard.app.ui.openbutton

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.strangeparticle.springboard.app.aws.S3UrlParser

private const val INVALID_S3_URL_MESSAGE =
    "Expected a virtual-hosted S3 URL: https://<bucket>.s3.<region>.amazonaws.com/<key>"

@Composable
fun OpenFromS3Dialog(
    onConfirm: (OpenFromS3Request) -> Unit,
    onDismiss: () -> Unit,
    defaultAwsProfile: String = "",
) {
    var url by remember { mutableStateOf("") }
    var awsProfile by remember { mutableStateOf(defaultAwsProfile) }
    val trimmedUrl = url.trim()
    val trimmedProfile = awsProfile.trim()
    val urlIsValid = trimmedUrl.isNotBlank() && S3UrlParser.isValidVirtualHostedS3Url(trimmedUrl)
    val showUrlError = trimmedUrl.isNotBlank() && !urlIsValid
    val canConfirm = urlIsValid && trimmedProfile.isNotBlank()
    val urlFocusRequester = remember { FocusRequester() }
    val confirmIfValid = {
        if (canConfirm) {
            onConfirm(OpenFromS3Request(url = trimmedUrl, awsProfile = trimmedProfile))
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        urlFocusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyUp) {
                        return@onPreviewKeyEvent false
                    }
                    when (keyEvent.key) {
                        Key.Enter, Key.NumPadEnter -> {
                            confirmIfValid()
                            canConfirm
                        }
                        else -> false
                    }
                },
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    "Open from S3",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.shapes.extraSmall)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        ) {
                            if (url.isEmpty()) {
                                Text(
                                    "https://<bucket>.s3.<region>.amazonaws.com/<key>",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(urlFocusRequester),
                )
                if (showUrlError) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        INVALID_S3_URL_MESSAGE,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "AWS Profile",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                BasicTextField(
                    value = awsProfile,
                    onValueChange = { awsProfile = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = { confirmIfValid() },
                        onDone = { confirmIfValid() },
                    ),
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.shapes.extraSmall)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        ) {
                            if (awsProfile.isEmpty()) {
                                Text("default", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .height(32.dp)
                            .defaultMinSize(minWidth = 78.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            disabledElevation = 0.dp,
                        ),
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = { confirmIfValid() },
                        enabled = canConfirm,
                        modifier = Modifier
                            .height(32.dp)
                            .defaultMinSize(minWidth = 78.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    ) {
                        Text("Open", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
