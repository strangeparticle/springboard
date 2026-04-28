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
        return fromDto(dto, source, jsonSource = jsonString)
    }

    private fun fromDto(dto: SpringboardDto, source: String, jsonSource: String): Springboard {
        require(dto.environments.none { it.id.equals(ALL_ENVS_ENVIRONMENT_ID, ignoreCase = true) }) {
            "Environment id '$ALL_ENVS_ENVIRONMENT_ID' is reserved for non-environment-specific activators and cannot be used as a configured environment id."
        }

        val environmentIds = dto.environments.map { it.id }.toSet()
        val appIds = dto.apps.map { it.id }.toSet()
        val resourceIds = dto.resources.map { it.id }.toSet()

        val appGroupIds = mutableSetOf<String>()
        for (groupDto in dto.appGroups) {
            require(appGroupIds.add(groupDto.id)) {
                "Duplicate appGroup id: '${groupDto.id}'"
            }
        }
        for (appDto in dto.apps) {
            if (appDto.appGroupId != null) {
                require(appDto.appGroupId in appGroupIds) {
                    "App '${appDto.id}' references non-existent appGroup: '${appDto.appGroupId}'"
                }
            }
        }

        val activators = dto.activators.map { activatorDto ->
            require(activatorDto.environmentId != "*") {
                "Activator uses '*' for environmentId, which is no longer supported. Use '$ALL_ENVS_ENVIRONMENT_ID' instead for non-environment-specific activators."
            }
            val normalizedEnvironmentId = canonicalizeEnvironmentId(activatorDto.environmentId)
            require(normalizedEnvironmentId in environmentIds || normalizedEnvironmentId == ALL_ENVS_ENVIRONMENT_ID) {
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
                    environmentId = normalizedEnvironmentId,
                    url = activatorDto.url
                )
                is UrlTemplateActivatorDto -> UrlTemplateActivator(
                    appId = activatorDto.appId,
                    resourceId = activatorDto.resourceId,
                    environmentId = normalizedEnvironmentId,
                    urlTemplate = activatorDto.urlTemplate
                )
                is CommandActivatorDto -> CommandActivator(
                    appId = activatorDto.appId,
                    resourceId = activatorDto.resourceId,
                    environmentId = normalizedEnvironmentId,
                    commandTemplate = activatorDto.commandTemplate
                )
            }
        }

        val indexes = buildIndexes(activators)

        val guidanceData = dto.guidanceData.map { guidanceDto ->
            require(guidanceDto.environmentId != "*") {
                "Guidance data uses '*' for environmentId, which is no longer supported. Use '$ALL_ENVS_ENVIRONMENT_ID' instead for non-environment-specific guidance."
            }
            val normalizedEnvironmentId = canonicalizeEnvironmentId(guidanceDto.environmentId)
            require(normalizedEnvironmentId in environmentIds || normalizedEnvironmentId == ALL_ENVS_ENVIRONMENT_ID) {
                "Guidance data references non-existent environment: '${guidanceDto.environmentId}'"
            }
            require(guidanceDto.appId in appIds) {
                "Guidance data references non-existent app: '${guidanceDto.appId}'"
            }
            require(guidanceDto.resourceId in resourceIds) {
                "Guidance data references non-existent resource: '${guidanceDto.resourceId}'"
            }
            GuidanceData(
                environmentId = normalizedEnvironmentId,
                appId = guidanceDto.appId,
                resourceId = guidanceDto.resourceId,
                guidanceLines = guidanceDto.guidanceLines
            )
        }

        for (guidance in guidanceData) {
            val coordinate = Coordinate(guidance.environmentId, guidance.appId, guidance.resourceId)
            require(indexes.activatorByCoordinate.containsKey(coordinate)) {
                "Guidance data references coordinate without an activator: env='${guidance.environmentId}', app='${guidance.appId}', resource='${guidance.resourceId}'"
            }
        }

        val guidanceByCoordinate = guidanceData.associateBy {
            Coordinate(it.environmentId, it.appId, it.resourceId)
        }

        val indexesWithGuidance = indexes.copy(guidanceByCoordinate = guidanceByCoordinate)

        return Springboard(
            name = dto.name,
            environments = dto.environments.map { Environment(it.id, it.name) },
            apps = dto.apps.map { App(it.id, it.name, appGroupId = it.appGroupId) },
            resources = dto.resources.map { Resource(it.id, it.name) },
            activators = activators,
            guidanceData = guidanceData,
            displayHints = dto.displayHints?.let { DisplayHints(it.width, it.height) },
            indexes = indexesWithGuidance,
            source = source,
            lastLoadTime = currentTimeMillis(),
            jsonSource = jsonSource,
            appGroups = dto.appGroups.map { AppGroup(it.id, it.description) },
        )
    }

    private fun canonicalizeEnvironmentId(environmentId: String): String =
        if (environmentId.equals(ALL_ENVS_ENVIRONMENT_ID, ignoreCase = true)) ALL_ENVS_ENVIRONMENT_ID else environmentId

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
