package com.strangeparticle.springboard.app

/**
 * Web (wasmJs) application entry point. The full Compose web wiring lives in the
 * shared module's wasmJs source set (runSpringboardWeb) so it can reach the
 * shared library's internal declarations; this module only provides the
 * executable entry and the browser bootstrap resources (index.html).
 */
fun main() {
    runSpringboardWeb()
}
