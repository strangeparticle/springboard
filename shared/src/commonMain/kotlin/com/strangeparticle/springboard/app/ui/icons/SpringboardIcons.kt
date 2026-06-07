package com.strangeparticle.springboard.app.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The fixed set of Material icons the app uses, hand-bundled as [ImageVector]s so the project
 * no longer depends on the deprecated `material-icons-core`/`material-icons-extended` artifacts.
 *
 * Each path is the official Material Icons SVG `d` data: the filled theme unless noted, with
 * [Info] and [Warning] taking the outlined theme to match their previous `Icons.Outlined.*`
 * usage, and [ArrowBack] auto-mirrored to match `Icons.AutoMirrored.Filled.ArrowBack`.
 *
 * Vectors are built lazily so only the icons actually accessed are constructed.
 */
object SpringboardIcons {
    val Add: ImageVector by lazy {
        materialIconVector("Add", "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z")
    }

    val ArrowBack: ImageVector by lazy {
        materialIconVector(
            name = "ArrowBack",
            pathData = "M20 11H7.83l5.59-5.59L12 4l-8 8l8 8l1.41-1.41L7.83 13H20z",
            autoMirror = true,
        )
    }

    val ArrowDropDown: ImageVector by lazy {
        materialIconVector("ArrowDropDown", "m7 10l5 5l5-5z")
    }

    val AutoAwesome: ImageVector by lazy {
        materialIconVector(
            name = "AutoAwesome",
            pathData = "m19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25z" +
                "m-7.5.5L9 4L6.5 9.5L1 12l5.5 2.5L9 20l2.5-5.5L17 12z" +
                "M19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25z",
        )
    }

    val BugReport: ImageVector by lazy {
        materialIconVector(
            name = "BugReport",
            pathData = "M20 8h-2.81a6 6 0 0 0-1.82-1.96L17 4.41L15.59 3l-2.17 2.17" +
                "C12.96 5.06 12.49 5 12 5s-.96.06-1.41.17L8.41 3L7 4.41l1.62 1.63" +
                "C7.88 6.55 7.26 7.22 6.81 8H4v2h2.09c-.05.33-.09.66-.09 1v1H4v2h2v1" +
                "c0 .34.04.67.09 1H4v2h2.81c1.04 1.79 2.97 3 5.19 3s4.15-1.21 5.19-3H20v-2" +
                "h-2.09c.05-.33.09-.66.09-1v-1h2v-2h-2v-1c0-.34-.04-.67-.09-1H20z" +
                "m-6 8h-4v-2h4zm0-4h-4v-2h4z",
        )
    }

    val Check: ImageVector by lazy {
        materialIconVector("Check", "M9 16.17L4.83 12l-1.42 1.41L9 19L21 7l-1.41-1.41z")
    }

    val Close: ImageVector by lazy {
        materialIconVector(
            name = "Close",
            pathData = "M19 6.41L17.59 5L12 10.59L6.41 5L5 6.41L10.59 12L5 17.59L6.41 19" +
                "L12 13.41L17.59 19L19 17.59L13.41 12z",
        )
    }

    val CloudDownload: ImageVector by lazy {
        materialIconVector(
            name = "CloudDownload",
            pathData = "M19.35 10.04A7.49 7.49 0 0 0 12 4C9.11 4 6.6 5.64 5.35 8.04" +
                "A5.994 5.994 0 0 0 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5" +
                "c0-2.64-2.05-4.78-4.65-4.96M17 13l-5 5l-5-5h3V9h4v4z",
        )
    }

    val ContentCopy: ImageVector by lazy {
        materialIconVector(
            name = "ContentCopy",
            pathData = "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12zm3 4H8c-1.1 0-2 .9-2 2v14" +
                "c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2m0 16H8V7h11z",
        )
    }

    val DragHandle: ImageVector by lazy {
        materialIconVector("DragHandle", "M20 9H4v2h16zM4 15h16v-2H4z")
    }

    val Info: ImageVector by lazy {
        materialIconVector(
            name = "Info",
            pathData = "M11 7h2v2h-2zm0 4h2v6h-2zm1-9C6.48 2 2 6.48 2 12s4.48 10 10 10" +
                "s10-4.48 10-10S17.52 2 12 2m0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8s8 3.59 8 8" +
                "s-3.59 8-8 8",
        )
    }

    val Lock: ImageVector by lazy {
        materialIconVector(
            name = "Lock",
            pathData = "M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10" +
                "c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2m-6 9c-1.1 0-2-.9-2-2" +
                "s.9-2 2-2s2 .9 2 2s-.9 2-2 2m3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1" +
                "s3.1 1.39 3.1 3.1z",
        )
    }

    val Refresh: ImageVector by lazy {
        materialIconVector(
            name = "Refresh",
            pathData = "M17.65 6.35A7.96 7.96 0 0 0 12 4c-4.42 0-7.99 3.58-7.99 8" +
                "s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0 1 12 18" +
                "c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4z",
        )
    }

    val Settings: ImageVector by lazy {
        materialIconVector(
            name = "Settings",
            pathData = "M19.14 12.94c.04-.3.06-.61.06-.94c0-.32-.02-.64-.07-.94l2.03-1.58" +
                "a.49.49 0 0 0 .12-.61l-1.92-3.32a.49.49 0 0 0-.59-.22l-2.39.96" +
                "c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.484.484 0 0 0-.48-.41h-3.84" +
                "c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96" +
                "c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58" +
                "c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61" +
                "l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54" +
                "c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54" +
                "c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32" +
                "c.12-.22.07-.47-.12-.61zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6" +
                "s1.62-3.6 3.6-3.6s3.6 1.62 3.6 3.6s-1.62 3.6-3.6 3.6",
        )
    }

    val Warning: ImageVector by lazy {
        materialIconVector(
            name = "Warning",
            pathData = "M12 5.99L19.53 19H4.47zM12 2L1 21h22zm1 14h-2v2h2zm0-6h-2v4h2z",
        )
    }
}
