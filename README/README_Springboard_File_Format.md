# Springboard File Format

Springboard is configured by a single JSON file.

On the desktop app you open it with standard "open file" behavior (**Cmd+O**, or File → Open, etc).

The public example file in this repo is `springboard-example.json`.

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
| `activators[].type` | yes | `"url"` or `"cmd"` |
| `activators[].appId` | yes | Must match a declared `apps[].id` |
| `activators[].resourceId` | yes | Must match a declared `resources[].id` |
| `activators[].environmentId` | yes | Must match a declared `environments[].id`, or `"*"` for all environments |
| `activators[].url` | when `type="url"` | Literal URL, opened in the default browser |
| `activators[].commandTemplate` | when `type="cmd"` | Shell command, desktop only |
| `displayHints.width` | optional | Preferred window width in pixels |
| `displayHints.height` | optional | Preferred window height in pixels |
| `guidanceData` | optional | List of per-coordinate guidance entries (see below) |
| `guidanceData[].environmentId` | yes | Must match a declared `environments[].id`, or `"*"` for all environments |
| `guidanceData[].appId` | yes | Must match a declared `apps[].id` |
| `guidanceData[].resourceId` | yes | Must match a declared `resources[].id` |
| `guidanceData[].guidanceLines` | yes | Ordered list of plain-text strings shown in the guidance tooltip |

## Guidance Data

Guidance data provides optional, per-coordinate instructional text that appears in a tooltip-style overlay when the user hovers a populated cell. It is stored separately from activators to keep both sections compact and easy to hand-edit.

Each guidance entry is associated with a coordinate (environment + app + resource) that **must** have a corresponding activator — guidance for empty cells is rejected at load time.

```json
"guidanceData": [
  {
    "environmentId": "prod",
    "appId": "my-service",
    "resourceId": "grafana",
    "guidanceLines": [
      "Use the prod AWS account before opening this link.",
      "Select the us-west-2 region.",
      "Open the Log Groups section after landing."
    ]
  }
]
```

- `guidanceLines` are rendered as plain text. Formatting (markdown, etc.) is not supported.
- The tooltip is selectable and includes a copy icon for easy clipboard access.
- Guidance data is entirely optional — files without the `guidanceData` field continue to work unchanged.
- Guidance data is maintained by hand-editing the JSON file, just like activators.

## Wildcard Environment

Use `"environmentId": "*"` on an activator (or guidance entry) to make it apply to all declared environments. The `*` environment is not declared in the `environments` array — it is a built-in shorthand.

```json
"activators": [
  { "type": "url", "appId": "my-service", "resourceId": "github", "environmentId": "*", "url": "https://github.com/..." }
]
```

- A wildcard activator appears in the grid and is activatable in every environment.
- The `*` environment does not appear in the environment dropdown.
- A given (app, resource) pair cannot have both a `*` activator and an environment-specific activator — this is rejected at load time.
- Guidance data supports `*` with the same rules.

## Rules

- **Not every combination needs an activator.** Cells without an activator are simply empty. Only add entries for combinations you want to activate.
- **IDs are case-sensitive.** `"prod"` and `"Prod"` are different ids.
- **Command activators are desktop-only.**
- **Order matters.** Environments, apps, and resources are rendered in the order they appear in the config — put the ones you use most first.

A worked example is included at `springboard-example.json`.
