package com.strangeparticle.springboard.app.domain.model

data class Springboard(
    val name: String,
    val environments: List<Environment>,
    val apps: List<App>,
    val resources: List<Resource>,
    val activators: List<Activator>,
    val guidanceData: List<GuidanceData>,
    val displayHints: DisplayHints?,
    val indexes: SpringboardIndexes,
    val source: String,
    val lastLoadTime: Long,
    val jsonSource: String,     // the source-file used to build this springboard
    val appGroups: List<AppGroup> = emptyList(),
)
