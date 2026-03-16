package com.strangeparticle.springboard.app.settings

actual fun detectRuntimeEnvironment(): RuntimeEnvironment {
    val osName = System.getProperty("os.name")?.lowercase() ?: ""
    return when {
        osName.contains("mac") || osName.contains("darwin") -> RuntimeEnvironment.DesktopOsx
        osName.contains("linux") || osName.contains("nux") -> RuntimeEnvironment.DesktopLinux
        osName.contains("win") -> RuntimeEnvironment.DesktopWindows
        else -> RuntimeEnvironment.Unknown
    }
}
