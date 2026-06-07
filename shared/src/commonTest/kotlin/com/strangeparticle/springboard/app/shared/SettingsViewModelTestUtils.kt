package com.strangeparticle.springboard.app.shared

import com.strangeparticle.springboard.app.settings.SettingsManager
import com.strangeparticle.springboard.app.viewmodel.SettingsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode

/**
 * HttpClient that always returns 200 with an empty body. Used by tests that
 * construct a [SettingsViewModel] but don't exercise any HTTP-driven settings
 * (e.g. async model dropdowns).
 */
fun stubHttpClientForTests(): HttpClient =
    HttpClient(MockEngine { respond(content = "", status = HttpStatusCode.OK) })

fun createSettingsViewModelForTest(
    settingsManager: SettingsManager,
    httpClient: HttpClient = stubHttpClientForTests(),
): SettingsViewModel = SettingsViewModel(settingsManager, httpClient)
