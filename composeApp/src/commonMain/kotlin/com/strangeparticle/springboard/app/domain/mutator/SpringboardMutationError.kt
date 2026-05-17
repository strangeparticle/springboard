package com.strangeparticle.springboard.app.domain.mutator

/** Structured failure for model-correctable Springboard mutation/precondition errors. */
internal class SpringboardMutationError(
    val errorMessage: String,
    val code: String,
) : RuntimeException(errorMessage)
