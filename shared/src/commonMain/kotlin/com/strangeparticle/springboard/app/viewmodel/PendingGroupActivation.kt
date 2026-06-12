package com.strangeparticle.springboard.app.viewmodel

import com.strangeparticle.springboard.app.domain.model.Activator

/**
 * A row/column header group activation that has been resolved but not yet executed,
 * awaiting user confirmation. Clicking a row or column header can fire many activators
 * at once (opening many URLs / running many commands), so the resolved activators are
 * held here while a confirmation dialog is shown.
 *
 * The resolved [activators] are captured at click time and replayed verbatim on confirm,
 * so the count shown in the dialog always matches what actually runs. [count] is exposed
 * separately for convenience so the UI does not depend on the activator list shape.
 */
data class PendingGroupActivation(
    val activators: List<Activator>,
) {
    val count: Int get() = activators.size
}
