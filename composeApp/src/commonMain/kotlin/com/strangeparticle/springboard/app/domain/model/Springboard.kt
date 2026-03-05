package com.strangeparticle.springboard.app.domain.model

data class Springboard(
    val name: String,
    val environments: List<Environment>,
    val apps: List<App>,
    val resources: List<Resource>,
    val activators: List<Activator>,
    val displayHints: DisplayHints?,
    val indexes: SpringboardIndexes,
    val source: String,
    val lastLoadTime: Long
)
