package com.strangeparticle.springboard.app.domain.factory

import com.strangeparticle.springboard.app.domain.factory.dto.AppDto
import com.strangeparticle.springboard.app.domain.factory.dto.AppGroupDto
import com.strangeparticle.springboard.app.domain.factory.dto.CommandActivatorDto
import com.strangeparticle.springboard.app.domain.factory.dto.EnvironmentDto
import com.strangeparticle.springboard.app.domain.factory.dto.GuidanceDataDto
import com.strangeparticle.springboard.app.domain.factory.dto.ResourceDto
import com.strangeparticle.springboard.app.domain.factory.dto.SpringboardDto
import com.strangeparticle.springboard.app.domain.factory.dto.UrlActivatorDto
import com.strangeparticle.springboard.app.domain.factory.dto.UrlTemplateActivatorDto
import com.strangeparticle.springboard.app.domain.model.CommandActivator
import com.strangeparticle.springboard.app.domain.model.Springboard
import com.strangeparticle.springboard.app.domain.model.UrlActivator
import com.strangeparticle.springboard.app.domain.model.UrlTemplateActivator
import kotlinx.serialization.json.Json

/**
 * Serializes a [Springboard] (domain) back to JSON. Inverse of
 * [SpringboardFactory.fromJson]: builds a [SpringboardDto] from the domain object
 * and encodes it via kotlinx.serialization's standard pretty-print.
 *
 * Field order in the output follows the declared field order in [SpringboardDto].
 * Optional fields whose value matches their declared default (e.g. an app with no
 * `appGroupId`, an empty `guidanceData` list) are omitted from the output.
 */
object SpringboardJsonWriter {

    private val json: Json = Json {
        classDiscriminator = "type"
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    fun toJson(springboard: Springboard): String =
        json.encodeToString(SpringboardDto.serializer(), toDto(springboard))

    private fun toDto(springboard: Springboard): SpringboardDto = SpringboardDto(
        name = springboard.name,
        environments = springboard.environments.map { EnvironmentDto(it.id, it.name) },
        apps = springboard.apps.map { AppDto(it.id, it.name, appGroupId = it.appGroupId) },
        resources = springboard.resources.map { ResourceDto(it.id, it.name) },
        activators = springboard.activators.map { activator ->
            when (activator) {
                is UrlActivator -> UrlActivatorDto(
                    appId = activator.appId,
                    resourceId = activator.resourceId,
                    environmentId = activator.environmentId,
                    url = activator.url,
                )
                is UrlTemplateActivator -> UrlTemplateActivatorDto(
                    appId = activator.appId,
                    resourceId = activator.resourceId,
                    environmentId = activator.environmentId,
                    urlTemplate = activator.urlTemplate,
                )
                is CommandActivator -> CommandActivatorDto(
                    appId = activator.appId,
                    resourceId = activator.resourceId,
                    environmentId = activator.environmentId,
                    commandTemplate = activator.commandTemplate,
                )
            }
        },
        guidanceData = springboard.guidanceData.map { guidance ->
            GuidanceDataDto(
                environmentId = guidance.environmentId,
                appId = guidance.appId,
                resourceId = guidance.resourceId,
                guidanceLines = guidance.guidanceLines,
            )
        },
        appGroups = springboard.appGroups.map { AppGroupDto(it.id, it.description) },
    )
}
