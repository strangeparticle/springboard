package com.strangeparticle.springboard.app.legal

fun loadBundledLicenseText(): String {
    val projectLicenseText = loadBundledClasspathLegalResource("/legal/LICENSE.txt")
    val thirdPartyNoticesText = loadBundledClasspathLegalResource("/legal/THIRD_PARTY_NOTICES.txt")

    return buildString {
        append("Springboard License\n")
        append("===================\n\n")
        append(projectLicenseText)
        append("\n\n")
        append("Third-Party Notices\n")
        append("===================\n\n")
        append(thirdPartyNoticesText)
        append("\n\n")
        append("Bundled Java Runtime\n")
        append("====================\n\n")
        append(BUNDLED_JAVA_RUNTIME_NOTICE)
    }
}

private val BUNDLED_JAVA_RUNTIME_NOTICE = """
    Springboard desktop distributions include a bundled Java runtime.
    The full runtime license and notice files are included in the application package under the embedded runtime's legal directory.
""".trimIndent()

private fun loadBundledClasspathLegalResource(resourcePath: String): String {
    val resourceStream = object {}.javaClass.getResourceAsStream(resourcePath)
    if (resourceStream == null) {
        throw IllegalStateException("Missing bundled legal resource: $resourcePath")
    }

    return resourceStream.bufferedReader().use { bufferedReader ->
        bufferedReader.readText()
    }
}
