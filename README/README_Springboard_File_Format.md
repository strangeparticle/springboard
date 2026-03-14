# Springboard File Format

Springboard is configured by a single JSON file.

On the desktop app you open it with standard "open file" behavior (**Cmd+O**, or File → Open, etc).

For the web app the URL to a springboard file is configured as an environment variable.

## Format

The config file must be valid JSON. All top-level fields are required unless noted. All `id` values used inside `activators` must exactly match a declared id in `environments`, `apps`, or `resources` — mismatched ids are rejected at load time with a descriptive error.

```
{
  "name": "My Springboard",

  "environments": [
    { "id": "staging", "name": "Staging" },
    { "id": "prod",    "name": "Prod" }
  ],

  "apps": [
    { "id": "my-service", "name": "My Service" }
  ],

  "resources": [
    { "id": "github",  "name": "GitHub" },
    { "id": "grafana", "name": "Grafana" }
  ],

  "activators": [
    { "type": "url",     "appId": "my-service", "resourceId": "github",  "environmentId": "staging", "url": "https://github.com/..." },
    { "type": "url",     "appId": "my-service", "resourceId": "grafana", "environmentId": "staging", "url": "https://grafana.example.com/..." },
    { "type": "url",     "appId": "my-service", "resourceId": "github",  "environmentId": "prod",    "url": "https://github.com/..." },
    { "type": "cmd",     "appId": "my-service", "resourceId": "github",  "environmentId": "staging",
      "commandTemplate": "\"/Applications/IntelliJ IDEA CE.app/Contents/MacOS/idea\" /path/to/project" }
  ]
}
```

## Fields

| Field | Required | Notes |
|---|---|---|
| `name` | yes | Display name shown in the status bar |
| `environments` | yes | Ordered list — rendered in the order declared |
| `environments[].id` | yes | Unique identifier, referenced by activators |
| `environments[].name` | yes | Display label |
| `apps` | yes | Ordered list — rendered in the order declared |
| `apps[].id` | yes | |
| `apps[].name` | yes | |
| `resources` | yes | Ordered list — rendered in the order declared |
| `resources[].id` | yes | |
| `resources[].name` | yes | |
| `activators` | yes | One entry per (environment, app, resource) combination you want to activate |
| `activators[].type` | yes | `"url"`, `"urlTemplate"`, or `"cmd"` |
| `activators[].appId` | yes | Must match a declared `apps[].id` |
| `activators[].resourceId` | yes | Must match a declared `resources[].id` |
| `activators[].environmentId` | yes | Must match a declared `environments[].id` |
| `activators[].url` | when `type="url"` | Literal URL, opened in the default browser |
| `activators[].urlTemplate` | when `type="urlTemplate"` | URL with interpolation (Phase 2) |
| `activators[].commandTemplate` | when `type="cmd"` | Shell command, desktop only |
| `displayHints.width` | optional | Preferred window width in pixels |
| `displayHints.height` | optional | Preferred window height in pixels |

## Rules

- **Not every combination needs an activator.** Cells without an activator are simply empty. Only add entries for combinations you want to activate.
- **IDs are case-sensitive.** `"prod"` and `"Prod"` are different ids.
- **Command activators are desktop-only.** They are ignored on the web target.
- **Order matters.** Environments, apps, and resources are rendered in the order they appear in the config — put the ones you use most first.

A worked example is included at `composeApp/src/wasmJsMain/resources/springboard.json`.
