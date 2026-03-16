package com.strangeparticle.springboard.app.settings

enum class RuntimeEnvironment {
    DesktopOsx,
    DesktopLinux,
    DesktopWindows,
    WASM,
    Unknown,
}

/**
 * Detects the current runtime environment. Platform-specific implementations
 * inspect the running OS to determine which [RuntimeEnvironment] applies.
 */
expect fun detectRuntimeEnvironment(): RuntimeEnvironment
