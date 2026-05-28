package com.strangeparticle.springboard.command

enum class SpringboardCommandErrorCode(val wireValue: String) {
    Unauthorized("unauthorized"),
    ProtocolVersionUnsupported("protocolVersionUnsupported"),
    InvalidRequest("invalidRequest"),
    ValidationFailed("validationFailed"),
    CoordinateNotFound("coordinateNotFound"),
    SpringboardNotLoaded("springboardNotLoaded"),
    TabNotFound("tabNotFound"),
    ActivationFailed("activationFailed"),
    InternalError("internalError");

    companion object {
        fun fromWireValue(value: String): SpringboardCommandErrorCode =
            entries.firstOrNull { it.wireValue == value } ?: InternalError
    }
}
