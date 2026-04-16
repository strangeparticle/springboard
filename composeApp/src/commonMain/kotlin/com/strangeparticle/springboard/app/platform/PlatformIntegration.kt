package com.strangeparticle.springboard.app.platform

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

expect fun openUrl(url: String)

expect fun openUrls(urls: List<String>)

expect fun openFileDialog(currentPath: String?): String?

expect fun readFileContents(path: String): String?

fun formatTimestamp(millis: Long): String =
    formatTimestamp(millis = millis, timeZone = TimeZone.currentSystemDefault())

fun formatTimestamp(millis: Long, timeZone: TimeZone): String {
    val localDateTime = Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone)
    val monthShortName = when (localDateTime.monthNumber) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        else -> "Dec"
    }
    val hour12 = when (val value = localDateTime.hour % 12) {
        0 -> 12
        else -> value
    }
    val minute = localDateTime.minute.toString().padStart(2, '0')
    val second = localDateTime.second.toString().padStart(2, '0')
    val amPm = if (localDateTime.hour < 12) "AM" else "PM"

    return "$monthShortName ${localDateTime.dayOfMonth}, ${localDateTime.year}, $hour12:$minute:$second $amPm"
}

expect fun getPlatformName(): String

expect fun executeCommand(command: String, onError: (String) -> Unit)

expect fun copyToClipboard(text: String)

expect fun openNewBrowserWindowIfAppropriate(): Boolean

expect fun saveLocalCopyAsFileDialog(suggestedName: String): String?

expect fun writeFileContents(path: String, contents: String): Boolean
