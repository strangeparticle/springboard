package com.strangeparticle.luther.client.provider

import androidx.compose.runtime.Composable
import com.strangeparticle.luther.client.AiProviderClient
import com.strangeparticle.springboard.app.settings.SettingsItem
import com.strangeparticle.springboard.app.settings.SettingsItemContext
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel

/**
 * Framework contract for an AI provider integration. Each provider object
 * (e.g. [com.strangeparticle.luther.client.provider.openai.OpenAiProvider])
 * bundles four facets:
 *
 *  1. Identity ([id], [displayName]).
 *  2. Persistence + diagnostics surface ([settingsItems]).
 *  3. Runtime client construction ([createClient]) — reads its own setup from
 *     the [SettingsItemContext] and returns a vendor-aware [AiProviderClient]
 *     that the chat code uses provider-neutrally.
 *  4. UI section ([settingsSectionComposable]) — a top-level composable that
 *     renders the provider's settings rows with whatever cascade rules apply
 *     to that specific provider's setup.
 *
 * Providers are compile-time-known and registered via [AiProviderRegistry].
 */
internal interface AiProvider {
    val id: String
    val displayName: String

    fun settingsItems(): List<SettingsItem<*>>
    fun createClient(context: SettingsItemContext): AiProviderClient

    /**
     * The currently-selected model id for this provider, read via [context]
     * from whichever per-provider setting holds it. Returns an empty string if
     * the user hasn't picked one yet.
     */
    fun currentModelId(context: SettingsItemContext): String

    /**
     * True if the provider has the credentials it needs to construct a working
     * client right now (env var or persisted setting set). Returns false if
     * the user still has to fill in the api key / profile / etc.
     */
    fun isConfigured(context: SettingsItemContext): Boolean

    val settingsSectionComposable: @Composable (SettingsViewModel) -> Unit
}
