package com.strangeparticle.springboard.app.shared

// Inlined rather than loaded from resource files because WASM's Karma/webpack test
// runner does not serve commonTest/resources, so classpath-based resource loading
// is only available on the JVM (desktop) target.
object TestFixtureJson {

    val URL_ONLY = """
    {
      "name": "URL Only Springboard",
      "environments": [{ "id": "dev", "name": "Dev" }],
      "apps": [{ "id": "app1", "name": "App" }],
      "resources": [{ "id": "res1", "name": "Resource" }],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com" }
      ]
    }
    """.trimIndent()

    val COMMAND_ACTIVATOR = """
    {
      "name": "Command Springboard",
      "environments": [{ "id": "dev", "name": "Dev" }],
      "apps": [{ "id": "app1", "name": "App" }],
      "resources": [{ "id": "res1", "name": "Resource" }],
      "activators": [
        { "type": "cmd", "appId": "app1", "resourceId": "res1", "environmentId": "dev", "commandTemplate": "echo test" }
      ]
    }
    """.trimIndent()
}
