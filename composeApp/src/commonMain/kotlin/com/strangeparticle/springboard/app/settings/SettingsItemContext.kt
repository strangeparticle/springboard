package com.strangeparticle.springboard.app.settings

import io.ktor.client.HttpClient

/**
 * Controlled surface a [SettingsItem] can reach through when it needs cross-cutting
 * access — HTTP for service-driven options, or sibling-setting reads for cases
 * where one setting's behaviour depends on another (e.g. an AI provider's
 * preferred-model loader needing to read the provider's api key).
 *
 * Most settings items don't take a context at all — booleans, strings, static
 * dropdowns just read and write their own value. Only the bases that genuinely
 * need cross-cutting access (today: `DropDownFromApiCallSettingsItem.loadOptions`,
 * `AiProvider.createClient`) accept this.
 *
 * Supplied by `SettingsViewModel.itemContext()`. The viewmodel takes an
 * [HttpClient] in its constructor and brokers access for the small number of
 * items that need it.
 */
interface SettingsItemContext {
    val httpClient: HttpClient
    fun <T : Any> get(item: SettingsItem<T>): T?
}
