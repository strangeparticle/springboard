package com.strangeparticle.springboard.app

/**
 * Desktop application entry point. The full Compose desktop wiring lives in the
 * shared module's JVM source set (runSpringboardDesktop) so it can reach the
 * shared library's internal declarations; this module only provides the
 * executable main class and the native distribution packaging configuration.
 */
fun main(args: Array<String>) {
    runSpringboardDesktop(args)
}
