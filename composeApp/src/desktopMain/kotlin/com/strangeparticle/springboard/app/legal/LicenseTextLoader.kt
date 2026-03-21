package com.strangeparticle.springboard.app.legal

fun loadBundledLicenseText(): String {
    val licenseResourcePath = "/legal/LICENSE.txt"
    val licenseStream = object {}.javaClass.getResourceAsStream(licenseResourcePath)
    if (licenseStream == null) {
        return "License text is unavailable. Expected bundled resource: $licenseResourcePath"
    }

    return licenseStream.bufferedReader().use { bufferedReader ->
        bufferedReader.readText()
    }
}
