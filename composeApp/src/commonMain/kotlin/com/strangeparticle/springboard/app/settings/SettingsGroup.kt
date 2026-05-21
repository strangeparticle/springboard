package com.strangeparticle.springboard.app.settings

/**
 * UI grouping for [SettingsItem]s. The variant *is* the grouping — the settings
 * screen renders one section per group present in the registry.
 *
 * Closed because provider implementations are compile-time and the user-facing
 * app has finite groups. Adding a new group is rare and intentional.
 */
enum class SettingsGroup {
    General,
    DesktopMacOS,
    DeveloperTools,
    AiAssistant,
}
