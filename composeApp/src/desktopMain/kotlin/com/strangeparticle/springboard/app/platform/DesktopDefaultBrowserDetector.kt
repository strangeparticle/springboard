package com.strangeparticle.springboard.app.platform

internal enum class DesktopBrowser {
    Chrome,
    Safari,
    Unsupported,
}

private enum class DesktopBrowserBundleId(val value: String) {
    Chrome("com.google.chrome"),
    Safari("com.apple.Safari"),
}

internal fun detectDefaultBrowser(): DesktopBrowser {
    val result = try {
        runShellCommand(
            "/usr/bin/defaults",
            "read",
            "com.apple.LaunchServices/com.apple.launchservices.secure",
            "LSHandlers"
        )
    } catch (e: Exception) {
        return DesktopBrowser.Unsupported
    }

    if (result.exitCode != 0) {
        return DesktopBrowser.Unsupported
    }

    val lauchServiceEntries = parseLaunchServicesEntries(result.stdout)
    val browserBundleId = extractDefaultBrowserBundleId(lauchServiceEntries)
    return when (browserBundleId) {
        DesktopBrowserBundleId.Chrome.value -> DesktopBrowser.Chrome
        DesktopBrowserBundleId.Safari.value -> DesktopBrowser.Safari
        else -> DesktopBrowser.Unsupported
    }
}

internal fun extractDefaultBrowserBundleId(launchServiceEntries: List<Map<String, String>>): String? {
    return launchServiceEntries.firstNotNullOfOrNull { entry ->
        when {
            entry["LSHandlerContentType"] == "com.apple.default-app.web-browser" -> entry["LSHandlerRoleAll"]
            else -> null
        }
    } ?: launchServiceEntries.firstNotNullOfOrNull { entry ->
        when (entry["LSHandlerURLScheme"]) {
            "https" -> entry["LSHandlerRoleAll"]
            else -> null
        }
    } ?: launchServiceEntries.firstNotNullOfOrNull { entry ->
        when (entry["LSHandlerURLScheme"]) {
            "http" -> entry["LSHandlerRoleAll"]
            else -> null
        }
    }
}

// Parses the output of: defaults read com.apple.LaunchServices/com.apple.launchservices.secure LSHandlers
// See: https://developer.apple.com/documentation/coreservices/launch_services
internal fun parseLaunchServicesEntries(launchServiceEntriesRaw: String): List<Map<String, String>> {
    val entries = mutableListOf<Map<String, String>>()
    val currentEntryLines = mutableListOf<String>()
    var depth = 0

    launchServiceEntriesRaw.lineSequence().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            return@forEach
        }

        val openCount = trimmed.count { it == '{' }
        val closeCount = trimmed.count { it == '}' }
        val startsEntry = depth == 0 && trimmed.startsWith("{")
        if (startsEntry) {
            currentEntryLines.clear()
        }
        if (depth > 0 || startsEntry) {
            currentEntryLines += line
        }
        depth += openCount - closeCount
        if (depth == 0 && currentEntryLines.isNotEmpty()) {
            entries += parseLaunchServicesEntry(currentEntryLines)
            currentEntryLines.clear()
        }
    }

    return entries
}

private fun parseLaunchServicesEntry(launchServiceEntriesAsLines: List<String>): Map<String, String> {
    val values = mutableMapOf<String, String>()
    var depth = 0

    launchServiceEntriesAsLines.forEach { line ->
        val trimmed = line.trim()
        if (depth == 1 && trimmed.contains('=') && trimmed.endsWith(';') && !trimmed.contains('{')) {
            val key = trimmed.substringBefore('=').trim()
            val value = trimmed.substringAfter('=').removeSuffix(";").trim().trim('"')
            values[key] = value
        }
        depth += trimmed.count { it == '{' } - trimmed.count { it == '}' }
    }

    return values
}
