package com.strangeparticle.springboard.app.loading

import com.strangeparticle.springboard.app.domain.model.Springboard

interface SpringboardLoader {
    fun load(source: String): Springboard?
}
