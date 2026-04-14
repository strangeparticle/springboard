package com.strangeparticle.springboard.app.persistence

class PersistenceServiceDefaultImpl(
    private val delegate: PersistenceService = createPersistenceServiceDefaultImpl(),
) : PersistenceService by delegate

internal expect fun createPersistenceServiceDefaultImpl(): PersistenceService
