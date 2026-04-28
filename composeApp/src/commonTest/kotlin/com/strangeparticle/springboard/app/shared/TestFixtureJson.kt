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

    val MULTI_ENV_WITH_COMMON = """
    {
      "name": "Multi-Env With Common",
      "environments": [
        { "id": "common", "name": "Common" },
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
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "common", "url": "https://example.com/common/app1/dash" },
        { "type": "url", "appId": "app1", "resourceId": "res2", "environmentId": "common", "url": "https://example.com/common/app1/logs" },
        { "type": "url", "appId": "app2", "resourceId": "res1", "environmentId": "common", "url": "https://example.com/common/app2/dash" },
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "preprod", "url": "https://example.com/preprod/app1/dash" },
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "prod", "url": "https://example.com/prod/app1/dash" }
      ]
    }
    """.trimIndent()

    val MULTI_ENV_WITH_GUIDANCE = """
    {
      "name": "Multi-Env With Guidance",
      "environments": [
        { "id": "common", "name": "Common" },
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
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "common", "url": "https://example.com/common/app1/dash" },
        { "type": "url", "appId": "app1", "resourceId": "res2", "environmentId": "common", "url": "https://example.com/common/app1/logs" },
        { "type": "url", "appId": "app2", "resourceId": "res1", "environmentId": "common", "url": "https://example.com/common/app2/dash" },
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "preprod", "url": "https://example.com/preprod/app1/dash" },
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "prod", "url": "https://example.com/prod/app1/dash" }
      ],
      "guidanceData": [
        {
          "environmentId": "common",
          "appId": "app1",
          "resourceId": "res1",
          "guidanceLines": ["Open the dashboard for this environment."]
        }
      ]
    }
    """.trimIndent()

    val MULTI_ENV_WITHOUT_COMMON = """
    {
      "name": "Multi-Env Without Common",
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

    val ALL_ENVS_ACTIVATORS = """
    {
      "name": "All-envs Springboard",
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
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "ALL", "url": "https://example.com/app1/dash" },
        { "type": "url", "appId": "app2", "resourceId": "res2", "environmentId": "prod", "url": "https://example.com/prod/app2/logs" }
      ]
    }
    """.trimIndent()

    val ALL_ENVS_GUIDANCE = """
    {
      "name": "All-envs Guidance Springboard",
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
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "ALL", "url": "https://example.com/app1/dash" }
      ],
      "guidanceData": [
        { "environmentId": "ALL", "appId": "app1", "resourceId": "res1", "guidanceLines": ["Step one.", "Step two."] }
      ]
    }
    """.trimIndent()

    val ALL_ENVS_AND_ENV_SPECIFIC_FOR_SAME_APP_RESOURCE = """
    {
      "name": "All-envs And Env Specific Coexistence",
      "environments": [
        { "id": "dev", "name": "Dev" },
        { "id": "prod", "name": "Production" }
      ],
      "apps": [{ "id": "app1", "name": "App" }],
      "resources": [{ "id": "res1", "name": "Resource" }],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "ALL", "url": "https://example.com/all" },
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/dev" }
      ]
    }
    """.trimIndent()

    val STAR_ENVIRONMENT_REJECTED = """
    {
      "name": "Star Rejected",
      "environments": [
        { "id": "dev", "name": "Dev" }
      ],
      "apps": [{ "id": "app1", "name": "App" }],
      "resources": [{ "id": "res1", "name": "Resource" }],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "*", "url": "https://example.com/star" }
      ]
    }
    """.trimIndent()

    val ALL_ENVS_RESERVED_AS_CONFIGURED_ENVIRONMENT = """
    {
      "name": "All-envs Reserved",
      "environments": [
        { "id": "dev", "name": "Dev" },
        { "id": "ALL", "name": "Should Be Rejected" }
      ],
      "apps": [{ "id": "app1", "name": "App" }],
      "resources": [{ "id": "res1", "name": "Resource" }],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/dev" }
      ]
    }
    """.trimIndent()

    val APP_GROUPS_WITH_SEPARATORS = """
    {
      "name": "App Groups With Separators",
      "environments": [{ "id": "dev", "name": "Dev" }],
      "apps": [
        { "id": "app1", "name": "App One",   "appGroupId": "groupA" },
        { "id": "app2", "name": "App Two",   "appGroupId": "groupB" },
        { "id": "app3", "name": "App Three", "appGroupId": "groupA" },
        { "id": "app4", "name": "App Four" }
      ],
      "resources": [{ "id": "res1", "name": "Resource" }],
      "activators": [
        { "type": "url", "appId": "app1", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/app1" },
        { "type": "url", "appId": "app2", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/app2" },
        { "type": "url", "appId": "app3", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/app3" },
        { "type": "url", "appId": "app4", "resourceId": "res1", "environmentId": "dev", "url": "https://example.com/app4" }
      ],
      "appGroups": [
        { "id": "groupA", "description": "Group A" },
        { "id": "groupB", "description": "Group B" }
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
