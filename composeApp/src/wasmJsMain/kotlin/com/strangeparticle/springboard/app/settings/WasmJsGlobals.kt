package com.strangeparticle.springboard.app.settings

/**
 * Reads environment variable overrides from `window.springboardEnv`.
 *
 * Property names use the env var form derived from [SettingsKeyNaming.envVarName].
 * Values are strings. Unrecognized property names are ignored.
 *
 * See `springboard_resources/settings-system.md` for the full name derivation
 * rules and a table of current settings with their external names.
 */
fun readJsGlobalsAsEnvironmentVariables(): Map<String, String> {
    val keys = getSpringboardEnvKeys()
    if (keys.isEmpty()) return emptyMap()

    val result = mutableMapOf<String, String>()
    for (key in keys) {
        val value = getSpringboardEnvValue(key)
        if (value != null && value.isNotBlank()) {
            result[key] = value
        }
    }
    return result
}

@JsFun("""() => {
    if (typeof window.springboardEnv !== 'object' || window.springboardEnv === null) return [];
    return Object.keys(window.springboardEnv);
}""")
private external fun jsGetSpringboardEnvKeys(): JsArray<JsString>

@JsFun("""(key) => {
    if (typeof window.springboardEnv !== 'object' || window.springboardEnv === null) return null;
    var v = window.springboardEnv[key];
    if (v === undefined || v === null) return null;
    return String(v);
}""")
private external fun jsGetSpringboardEnvValue(key: JsString): JsString?

private fun getSpringboardEnvKeys(): List<String> {
    return try {
        val jsKeys = jsGetSpringboardEnvKeys()
        (0 until jsKeys.length).map { jsKeys[it].toString() }
    } catch (_: Throwable) {
        emptyList()
    }
}

private fun getSpringboardEnvValue(key: String): String? {
    return try {
        jsGetSpringboardEnvValue(key.toJsString())?.toString()
    } catch (_: Throwable) {
        null
    }
}
