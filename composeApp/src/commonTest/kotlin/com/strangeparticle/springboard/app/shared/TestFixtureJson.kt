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

    val ALTERNATIVE_URL_ONLY = """
    {
      "name": "Alternative Springboard",
      "environments": [{ "id": "staging", "name": "Staging" }],
      "apps": [{ "id": "web", "name": "Web App" }],
      "resources": [{ "id": "dashboard", "name": "Dashboard" }],
      "activators": [
        { "type": "url", "appId": "web", "resourceId": "dashboard", "environmentId": "staging", "url": "https://alt.example.com" }
      ]
    }
    """.trimIndent()

    val MULTI_ENV_WITH_ALL = """
    {
      "name": "Multi-Env With All",
      "environments": [
        { "id": "all", "name": "All" },
        { "id": "preprod", "name": "Preprod" },
        { "id": "prod", "name": "Production" }
      ],
      "apps": [
        { "id": "app1", "name": "App One" },
        { "id": "app2", "name": "App Two" }
      ],
      "resources": [
        { "id": "res1", "name": "Dashboard" },
        { "id": "res2", "name": "Logs" }
      ],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "all", "url": "https://example.com/all/app1/dash" },
        { "type": "url", "appId": "app1", "resourceId": "res2", "environmentId": "all", "url": "https://example.com/all/app1/logs" },
        { "type": "url", "appId": "app2", "resourceId": "res1", "environmentId": "all", "url": "https://example.com/all/app2/dash" },
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "preprod", "url": "https://example.com/preprod/app1/dash" },
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "prod", "url": "https://example.com/prod/app1/dash" }
      ]
    }
    """.trimIndent()

    val MULTI_ENV_WITH_GUIDANCE = """
    {
      "name": "Multi-Env With Guidance",
      "environments": [
        { "id": "all", "name": "All" },
        { "id": "preprod", "name": "Preprod" },
        { "id": "prod", "name": "Production" }
      ],
      "apps": [
        { "id": "app1", "name": "App One" },
        { "id": "app2", "name": "App Two" }
      ],
      "resources": [
        { "id": "res1", "name": "Dashboard" },
        { "id": "res2", "name": "Logs" }
      ],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "all", "url": "https://example.com/all/app1/dash" },
        { "type": "url", "appId": "app1", "resourceId": "res2", "environmentId": "all", "url": "https://example.com/all/app1/logs" },
        { "type": "url", "appId": "app2", "resourceId": "res1", "environmentId": "all", "url": "https://example.com/all/app2/dash" },
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "preprod", "url": "https://example.com/preprod/app1/dash" },
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "prod", "url": "https://example.com/prod/app1/dash" }
      ],
      "guidanceData": [
        {
          "environmentId": "all",
          "appId": "app1",
          "resourceId": "res1",
          "guidanceLines": ["Open the dashboard for this environment."]
        }
      ]
    }
    """.trimIndent()

    val MULTI_ENV_WITHOUT_ALL = """
    {
      "name": "Multi-Env Without All",
      "environments": [
        { "id": "preprod", "name": "Preprod" },
        { "id": "prod", "name": "Production" }
      ],
      "apps": [
        { "id": "app1", "name": "App One" }
      ],
      "resources": [
        { "id": "res1", "name": "Dashboard" }
      ],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "preprod", "url": "https://example.com/preprod/dash" },
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "prod", "url": "https://example.com/prod/dash" }
      ]
    }
    """.trimIndent()

    val WILDCARD_ACTIVATORS = """
    {
      "name": "Wildcard Springboard",
      "environments": [
        { "id": "dev", "name": "Dev" },
        { "id": "prod", "name": "Production" }
      ],
      "apps": [
        { "id": "app1", "name": "App One" },
        { "id": "app2", "name": "App Two" }
      ],
      "resources": [
        { "id": "res1", "name": "Dashboard" },
        { "id": "res2", "name": "Logs" }
      ],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "*", "url": "https://example.com/app1/dash" },
        { "type": "url", "appId": "app2", "resourceId": "res2", "environmentId": "prod", "url": "https://example.com/prod/app2/logs" }
      ]
    }
    """.trimIndent()

    val WILDCARD_GUIDANCE = """
    {
      "name": "Wildcard Guidance Springboard",
      "environments": [
        { "id": "dev", "name": "Dev" },
        { "id": "prod", "name": "Production" }
      ],
      "apps": [
        { "id": "app1", "name": "App One" }
      ],
      "resources": [
        { "id": "res1", "name": "Dashboard" }
      ],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "*", "url": "https://example.com/app1/dash" }
      ],
      "guidanceData": [
        { "environmentId": "*", "appId": "app1", "resourceId": "res1", "guidanceLines": ["Step one.", "Step two."] }
      ]
    }
    """.trimIndent()

    val WILDCARD_CONFLICT = """
    {
      "name": "Wildcard Conflict",
      "environments": [
        { "id": "dev", "name": "Dev" },
        { "id": "prod", "name": "Production" }
      ],
      "apps": [{ "id": "app1", "name": "App" }],
      "resources": [{ "id": "res1", "name": "Resource" }],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "*", "url": "https://example.com/star" },
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/dev" }
      ]
    }
    """.trimIndent()

    val WILDCARD_GUIDANCE_CONFLICT = """
    {
      "name": "Wildcard Guidance Conflict",
      "environments": [
        { "id": "dev", "name": "Dev" },
        { "id": "prod", "name": "Production" }
      ],
      "apps": [{ "id": "app1", "name": "App" }],
      "resources": [{ "id": "res1", "name": "Resource" }],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "*", "url": "https://example.com/star" }
      ],
      "guidanceData": [
        { "environmentId": "*", "appId": "app1", "resourceId": "res1", "guidanceLines": ["Wildcard guidance."] },
        { "environmentId": "dev", "appId": "app1", "resourceId": "res1", "guidanceLines": ["Dev guidance."] }
      ]
    }
    """.trimIndent()

    val MALFORMED_JSON = "{ this is not valid json"

    val INVALID_ACTIVATOR_REFERENCE = """
    {
      "name": "Invalid Reference",
      "environments": [{ "id": "dev", "name": "Dev" }],
      "apps": [{ "id": "app1", "name": "App" }],
      "resources": [{ "id": "res1", "name": "Resource" }],
      "activators": [
        { "type": "url", "appId": "nonexistent", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com" }
      ]
    }
    """.trimIndent()
}
