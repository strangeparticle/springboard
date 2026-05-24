package com.strangeparticle.springboard.app.viewmodel

/**
 * Result returned by the AI-assistant-callable activation methods on
 * [SpringboardViewModel]. Tool handlers map these to provider tool results
 * (success/error codes) so the assistant can report what happened.
 */
internal sealed class AssistantActivationOutcome {
    data class Success(val activatorCount: Int) : AssistantActivationOutcome()
    data object MissingTab : AssistantActivationOutcome()
    data object TabEmpty : AssistantActivationOutcome()
    data object NoActivatorsResolved : AssistantActivationOutcome()
}
