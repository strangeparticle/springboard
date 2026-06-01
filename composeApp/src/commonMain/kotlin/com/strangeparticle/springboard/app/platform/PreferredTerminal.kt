package com.strangeparticle.springboard.app.platform

/**
 * The terminal application a `term` activator opens. The [id] is the stable value
 * persisted by [com.strangeparticle.springboard.app.settings.items.core.PreferredTerminalSetting]
 * and matched against the user's preference at activation time.
 */
enum class PreferredTerminal(val id: String) {
    TerminalApp("terminal_app"),
    ITerm("iterm");

    companion object {
        /** Resolves a persisted id back to its enum value, falling back to
         *  [TerminalApp] for unknown ids so a stale setting never breaks activation. */
        fun fromId(id: String): PreferredTerminal =
            entries.firstOrNull { it.id == id } ?: TerminalApp
    }
}
