package com.strangeparticle.springboard.app.persistence

internal actual fun createPersistenceServiceDefaultImpl(): PersistenceService {
    return PersistenceServiceDesktopImpl()
}
