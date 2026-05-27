package com.strangeparticle.luther.toolcall

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

private val jsonCodecForRequests: Json = Json { ignoreUnknownKeys = true }

internal fun <T> decodeToolCallHandlerRequest(
    argumentsAsJsonString: String,
    serializer: KSerializer<T>,
): T = jsonCodecForRequests.decodeFromString(serializer, argumentsAsJsonString)
