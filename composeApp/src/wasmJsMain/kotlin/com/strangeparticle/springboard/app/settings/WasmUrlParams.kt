package com.strangeparticle.springboard.app.settings

@JsFun("() => window.location.search")
private external fun getLocationSearch(): JsString

/**
 * Parses URL query parameters into a map for the URL_PARAM source tier.
 *
 * URL param names use the param name form derived from [SettingsKeyNaming.urlParamName].
 * URL values that are themselves URLs must be percent-encoded by the embedding page.
 *
 * See `springboard_resources/settings-system.md` for the full name derivation
 * rules and a table of current settings with their external names.
 */
fun parseUrlParams(): Map<String, String> {
    val search = getLocationSearch().toString()
    if (search.isBlank() || search == "?") return emptyMap()

    val queryString = search.removePrefix("?")
    val params = mutableMapOf<String, String>()

    for (pair in queryString.split("&")) {
        val equalsIndex = pair.indexOf('=')
        if (equalsIndex < 0) continue

        val paramName = decodeUriComponent(pair.substring(0, equalsIndex))
        val paramValue = decodeUriComponent(pair.substring(equalsIndex + 1))

        if (paramName.isBlank() || paramValue.isBlank()) continue
        if (SettingsRegistry.findByUrlParamName(paramName) == null) continue

        params[paramName] = paramValue
    }

    return params
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
