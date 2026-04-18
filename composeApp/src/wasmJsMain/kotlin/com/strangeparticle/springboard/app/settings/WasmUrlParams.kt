package com.strangeparticle.springboard.app.settings

@JsFun("() => window.location.search")
private external fun getLocationSearch(): JsString

/**
 * Parses URL query parameters and maps them to settings values for the PARAMS source tier.
 *
 * URL param names use the param name form derived from [SettingsKeyNaming.urlParamName].
 * URL values that are themselves URLs must be percent-encoded by the embedding page.
 *
 * See `springboard_resources/settings-system.md` for the full name derivation
 * rules and a table of current settings with their external names.
 */
fun parseUrlParamsAsCommandLineArgs(): List<String> {
    val search = getLocationSearch().toString()
    if (search.isBlank() || search == "?") return emptyList()

    val queryString = search.removePrefix("?")
    val args = mutableListOf<String>()

    for (pair in queryString.split("&")) {
        val equalsIndex = pair.indexOf('=')
        if (equalsIndex < 0) continue

        val paramName = decodeUriComponent(pair.substring(0, equalsIndex))
        val paramValue = decodeUriComponent(pair.substring(equalsIndex + 1))

        if (paramName.isBlank() || paramValue.isBlank()) continue

        val entry = SettingsRegistry.findByUrlParamName(paramName) ?: continue
        val cliFlag = "--$paramName"

        if (entry.type == Boolean::class) {
            if (paramValue.equals("true", ignoreCase = true)) {
                args.add(cliFlag)
            }
        } else {
            args.add(cliFlag)
            args.add(paramValue)
        }
    }

    return args
}

@JsFun("(s) => decodeURIComponent(s)")
private external fun jsDecodeUriComponent(value: JsString): JsString

private fun decodeUriComponent(value: String): String {
    return try {
        jsDecodeUriComponent(value.toJsString()).toString()
    } catch (_: Throwable) {
        value
    }
}
