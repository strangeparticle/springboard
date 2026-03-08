package com.strangeparticle.springboard.app.platform

fun expandTildePath(path: String, homeDirectory: String): String =
    if (path.startsWith("~")) homeDirectory + path.substring(1) else path
