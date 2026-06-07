package com.strangeparticle.springboard.app.settings

/**
 * A lightweight data class wrapping a String path value.
 * Used for settings that represent file system paths.
 */
data class FilePath(val path: String) {
    override fun toString(): String = path
}
