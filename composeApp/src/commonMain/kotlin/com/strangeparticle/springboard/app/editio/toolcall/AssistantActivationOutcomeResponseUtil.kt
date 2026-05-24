package com.strangeparticle.springboard.app.editio.toolcall

import com.strangeparticle.springboard.app.editio.SpringboardToolCallHandlerResponse
import com.strangeparticle.springboard.app.editio.errorStatusResult
import com.strangeparticle.springboard.app.editio.successStatusResult
import com.strangeparticle.springboard.app.viewmodel.AssistantActivationOutcome

internal fun AssistantActivationOutcome.toToolCallResponse(tabId: String): SpringboardToolCallHandlerResponse =
    when (this) {
        is AssistantActivationOutcome.Success ->
            successStatusResult("Activated $activatorCount activator(s).")
        AssistantActivationOutcome.MissingTab ->
            errorStatusResult("No tab with id '$tabId'.", "missing_tab")
        AssistantActivationOutcome.TabEmpty ->
            errorStatusResult("Tab '$tabId' has no loaded springboard.", "tab_empty")
        AssistantActivationOutcome.NoActivatorsResolved ->
            errorStatusResult("No activators matched.", "no_activators_resolved")
    }
