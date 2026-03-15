package com.strangeparticle.springboard.app.domain.factory

import com.strangeparticle.springboard.app.domain.factory.dto.*
import com.strangeparticle.springboard.app.domain.model.*
import kotlinx.serialization.json.Json

internal val json = Json {
    ignoreUnknownKeys = true
    isLenient = false
    classDiscriminator = "type"
}

object SpringboardFactory {

    fun fromJson(jsonString: String, source: String): Springboard {
        val dto = json.decodeFromString<SpringboardDto>(jsonString)
        return fromDto(dto, source)
    }

    private fun fromDto(dto: SpringboardDto, source: String): Springboard {
        val environmentIds = dto.environments.map { it.id }.toSet()
        val appIds = dto.apps.map { it.id }.toSet()
        val resourceIds = dto.resources.map { it.id }.toSet()

        val activators = dto.activators.map { activatorDto ->
            require(activatorDto.environmentId in environmentIds) {
                "Activator references non-existent environment: '${activatorDto.environmentId}'"
            }
            require(activatorDto.appId in appIds) {
                "Activator references non-existent app: '${activatorDto.appId}'"
            }
            require(activatorDto.resourceId in resourceIds) {
                "Activator references non-existent resource: '${activatorDto.resourceId}'"
            }

            when (activatorDto) {
                is UrlActivatorDto -> UrlActivator(
                    appId = activatorDto.appId,
                    resourceId = activatorDto.resourceId,
                    environmentId = activatorDto.environmentId,
                    url = activatorDto.url
                )
                is UrlTemplateActivatorDto -> UrlTemplateActivator(
                    appId = activatorDto.appId,
                    resourceId = activatorDto.resourceId,
                    environmentId = activatorDto.environmentId,
                    urlTemplate = activatorDto.urlTemplate
                )
                is CommandActivatorDto -> CommandActivator(
                    appId = activatorDto.appId,
                    resourceId = activatorDto.resourceId,
                    environmentId = activatorDto.environmentId,
                    commandTemplate = activatorDto.commandTemplate
                )
            }
        }

        val indexes = buildIndexes(activators)

        val guidanceData = dto.guidanceData.map { guidanceDto ->
            require(guidanceDto.environmentId in environmentIds) {
                "Guidance data references non-existent environment: '${guidanceDto.environmentId}'"
            }
            require(guidanceDto.appId in appIds) {
                "Guidance data references non-existent app: '${guidanceDto.appId}'"
            }
            require(guidanceDto.resourceId in resourceIds) {
                "Guidance data references non-existent resource: '${guidanceDto.resourceId}'"
            }
            val coordinate = Coordinate(guidanceDto.environmentId, guidanceDto.appId, guidanceDto.resourceId)
            require(indexes.activatorByCoordinate.containsKey(coordinate)) {
                "Guidance data references coordinate without an activator: env='${guidanceDto.environmentId}', app='${guidanceDto.appId}', resource='${guidanceDto.resourceId}'"
            }
            GuidanceData(
                environmentId = guidanceDto.environmentId,
                appId = guidanceDto.appId,
                resourceId = guidanceDto.resourceId,
                guidanceLines = guidanceDto.guidanceLines
            )
        }

        val guidanceByCoordinate = guidanceData.associateBy {
            Coordinate(it.environmentId, it.appId, it.resourceId)
        }

        val indexesWithGuidance = indexes.copy(guidanceByCoordinate = guidanceByCoordinate)

        return Springboard(
            name = dto.name,
            environments = dto.environments.map { Environment(it.id, it.name) },
            apps = dto.apps.map { App(it.id, it.name) },
            resources = dto.resources.map { Resource(it.id, it.name) },
            activators = activators,
            guidanceData = guidanceData,
            displayHints = dto.displayHints?.let { DisplayHints(it.width, it.height) },
            indexes = indexesWithGuidance,
            source = source,
            lastLoadTime = currentTimeMillis()
        )
    }

    private fun buildIndexes(activators: List<Activator>): SpringboardIndexes {
        val byCoordinate = mutableMapOf<Coordinate, Activator>()
        val resourcesByApp = mutableMapOf<String, MutableSet<String>>()
        val appsByResource = mutableMapOf<String, MutableSet<String>>()
        val resourcesByEnvApp = mutableMapOf<Pair<String, String>, MutableSet<String>>()

        for (activator in activators) {
            val coordinate = Coordinate(activator.environmentId, activator.appId, activator.resourceId)
            byCoordinate[coordinate] = activator

            resourcesByApp.getOrPut(activator.appId) { mutableSetOf() }.add(activator.resourceId)
            appsByResource.getOrPut(activator.resourceId) { mutableSetOf() }.add(activator.appId)

            val envAppKey = activator.environmentId to activator.appId
            resourcesByEnvApp.getOrPut(envAppKey) { mutableSetOf() }.add(activator.resourceId)
        }

        return SpringboardIndexes(
            activatorByCoordinate = byCoordinate,
            activatableResourcesByApp = resourcesByApp,
            activatableAppsByResource = appsByResource,
            activatableResourcesByEnvApp = resourcesByEnvApp
        )
    }
}

internal expect fun currentTimeMillis(): Long
