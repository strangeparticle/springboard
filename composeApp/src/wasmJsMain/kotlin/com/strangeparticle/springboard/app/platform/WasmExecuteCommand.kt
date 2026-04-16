package com.strangeparticle.springboard.app.platform

actual fun executeCommand(command: String, onError: (String) -> Unit) {
    // Not supported on WASM
}
