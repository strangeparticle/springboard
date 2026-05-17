package com.strangeparticle.editio.session

internal interface AiSessionSnapshotProvider {
    fun getSnapshotJson(): String
}
