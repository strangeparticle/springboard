package com.strangeparticle.springboard.app.loading

import com.strangeparticle.springboard.app.domain.factory.SpringboardFactory
import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.platform.readFileContents

class FileSystemSpringboardLoader : SpringboardLoader {

    override fun load(source: String): Springboard? {
        val contents = readFileContents(source) ?: return null
        return SpringboardFactory.fromJson(contents, source)
    }
}
