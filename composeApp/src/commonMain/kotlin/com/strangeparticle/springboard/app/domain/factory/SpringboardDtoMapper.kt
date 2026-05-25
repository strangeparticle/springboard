package com.strangeparticle.springboard.app.domain.factory

import com.strangeparticle.springboard.app.domain.factory.dto.AppDto
import com.strangeparticle.springboard.app.domain.factory.dto.AppGroupDto
import com.strangeparticle.springboard.app.domain.factory.dto.CommandActivatorDto
import com.strangeparticle.springboard.app.domain.factory.dto.EnvironmentDto
import com.strangeparticle.springboard.app.domain.factory.dto.ResourceDto
import com.strangeparticle.springboard.app.domain.factory.dto.SpringboardDto
import com.strangeparticle.springboard.app.domain.factory.dto.UrlActivatorDto
import com.strangeparticle.springboard.app.domain.factory.dto.UrlTemplateActivatorDto
import com.strangeparticle.springboard.app.domain.model.CommandActivator
import com.strangeparticle.springboard.app.domain.model.Coordinate
import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.domain.model.UrlActivator
import com.strangeparticle.springboard.app.domain.model.UrlTemplateActivator

/**
 * Maps a domain [Springboard] back to a [SpringboardDto] suitable for serialization.
 * Inverse of [SpringboardFactory.fromDto]. Used by both [SpringboardJsonWriter] (to
 * emit on-disk JSON) and the AI editing snapshot machinery (to embed a tab's
 * springboard inside an `SpringboardAppSnapshot`).
 *
 * Lives in `domain.factory` rather than the AI feature package because the mapping
 * is purely between domain and DTO — it doesn't know anything about AI, snapshots,
 * or persistence sinks.
 */
internal fun springboardToDto(springboard: Springboard): SpringboardDto {
    val guidanceByCoordinate = springboard.guidanceData.associateBy {
        Coordinate(it.environmentId, it.appId, it.resourceId)
    }

    return SpringboardDto(
        name = springboard.name,
        environments = springboard.environments.map { EnvironmentDto(it.id, it.name) },
        apps = springboard.apps.map { AppDto(it.id, it.name, appGroupId = it.appGroupId) },
        resources = springboard.resources.map { ResourceDto(it.id, it.name) },
        activators = springboard.activators.map { activator ->
            val guidanceLines = guidanceByCoordinate[Coordinate(
                activator.environmentId,
                activator.appId,
                activator.resourceId,
            )]?.guidanceLines ?: emptyList()

            when (activator) {
                is UrlActivator -> UrlActivatorDto(
                    appId = activator.appId,
                    resourceId = activator.resourceId,
                    environmentId = activator.environmentId,
                    url = activator.url,
                    guidanceLines = guidanceLines,
                )
                is UrlTemplateActivator -> UrlTemplateActivatorDto(
                    appId = activator.appId,
                    resourceId = activator.resourceId,
                    environmentId = activator.environmentId,
                    urlTemplate = activator.urlTemplate,
                    guidanceLines = guidanceLines,
                )
                is CommandActivator -> CommandActivatorDto(
                    appId = activator.appId,
                    resourceId = activator.resourceId,
                    environmentId = activator.environmentId,
                    commandTemplate = activator.commandTemplate,
                    guidanceLines = guidanceLines,
                )
            }
        },
        appGroups = springboard.appGroups.map { AppGroupDto(it.id, it.description) },
    )
}
