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
      "commandTemplate": "\"/Applications/IntelliJ IDEA CE.app/Contents/MacOS/idea\" /path/to/project" },
    { "type": "term",    "appId": "my-service", "resourceId": "github",  "environmentId": "staging",
      "workingDirectory": "/path/to/project", "command": "git status" }
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
| `apps` | yes | Ordered list — rendered in the order declared, except apps with `appGroupId` are visually grouped (see "App Groups") |
| `apps[].id` | yes | |
| `apps[].name` | yes | |
| `apps[].appGroupId` | optional | Must match a declared `appGroups[].id` |
| `resources` | yes | Ordered list — rendered in the order declared |
| `resources[].id` | yes | |
| `resources[].name` | yes | |
| `activators` | yes | One entry per (environment, app, resource) combination you want to activate |
| `activators[].type` | yes | `"url"`, `"cmd"`, or `"term"` |
| `activators[].appId` | yes | Must match a declared `apps[].id` |
| `activators[].resourceId` | yes | Must match a declared `resources[].id` |
| `activators[].environmentId` | yes | Must match a declared `environments[].id`, or `"ALL"` for all environments |
| `activators[].url` | when `type="url"` | Literal URL, opened in the default browser |
| `activators[].commandTemplate` | when `type="cmd"` | Shell command, desktop only |
| `activators[].workingDirectory` | when `type="term"` | Directory the terminal opens in (`cd`'d into), desktop/macOS only |
| `activators[].command` | optional, `type="term"` | Command run in the new terminal session; omit to just open a prompt |
| `activators[].guidanceLines` | optional | Ordered list of plain-text strings shown in the guidance tooltip |
| `guidanceData` | optional | Legacy per-coordinate guidance entries; still accepted when loading existing files |
| `appGroups` | optional | Ordered list of app groups used to cluster app columns visually |
| `appGroups[].id` | yes | Unique identifier, referenced by `apps[].appGroupId` |
| `appGroups[].description` | yes | Free-text description (not currently shown in the UI) |

## Terminal Activators (`term`)

A `term` activator opens a terminal at `workingDirectory`, optionally running `command`, instead of embedding `osascript` boilerplate in a `cmd` activator. Two macOS settings govern the behavior:

- **Preferred terminal** — Terminal (default) or iTerm. If iTerm is selected but not installed, Springboard opens Terminal instead and shows a notice.
- **Open terminal activators in a new window** — when on, a new dedicated window is opened; when off, the command opens in a new tab in the current terminal window (or a new window if none is open). The window is brought to the front either way.

## Guidance Data

Guidance data provides optional instructional text that appears in a tooltip-style overlay when the user hovers a populated cell. Put `guidanceLines` directly on the activator whose cell should show the guidance.

Each guidance entry is associated with the activator's coordinate (environment + app + resource). Guidance for empty cells is rejected at load time when using the legacy top-level `guidanceData` field.

```json
{
  "type": "url",
  "appId": "my-service",
  "resourceId": "grafana",
  "environmentId": "prod",
  "url": "https://grafana.example.com/...",
  "guidanceLines": [
    "Use the prod AWS account before opening this link.",
    "Select the us-west-2 region.",
    "Open the Log Groups section after landing."
  ]
}
```

- `guidanceLines` are rendered as plain text. Formatting (markdown, etc.) is not supported.
- The tooltip is selectable and includes a copy icon for easy clipboard access.
- Guidance data is entirely optional — activators without `guidanceLines` continue to work unchanged.
- Existing files that use the old top-level `guidanceData` array still load correctly, but Springboard writes guidance back as `activators[].guidanceLines`.
- Guidance data is maintained by hand-editing the JSON file, just like the rest of each activator.

## All-envs Environment

Use `"environmentId": "ALL"` on an activator to make it apply to every environment. `ALL` is a reserved environment id — it cannot be declared in the `environments` array (the id is matched case-insensitively, so `"all"`, `"All"`, etc. are all rejected).

```json
"activators": [
  { "type": "url", "appId": "my-service", "resourceId": "github", "environmentId": "ALL", "url": "https://github.com/..." }
]
```

- All-envs activators are grouped under a dedicated "All envs" section in the grid, separate from the per-environment grid.
- The `ALL` environment does not appear in the environment dropdown.
- All-envs activators activate regardless of which environment is selected.
- Guidance attached to an ALL-envs activator follows the same rules.

## App Groups

App groups let you visually cluster related app columns in the grid. Declare an `appGroups` array, then assign each app to a group via the optional `appGroupId` field. Apps that share a group are rendered next to each other, and a blank separator column is drawn between adjacent groups (and between the last group and any ungrouped apps).

```json
"apps": [
  { "id": "service-a", "name": "Service A", "appGroupId": "billing" },
  { "id": "service-b", "name": "Service B", "appGroupId": "auth" },
  { "id": "service-c", "name": "Service C", "appGroupId": "billing" },
  { "id": "service-d", "name": "Service D" }
],
"appGroups": [
  { "id": "billing", "description": "Billing services" },
  { "id": "auth",    "description": "Auth services" }
]
```

- Groups are rendered in the order they appear in `appGroups`.
- Within a group, apps appear in the order they appear in `apps`.
- Apps without an `appGroupId` are rendered last, after a separator column.
- Declared groups with no member apps are silently skipped (no orphan separator).
- The `description` field is reserved for future UI use and is not currently displayed.
- Duplicate `appGroups[].id` values, or an `appGroupId` that does not match a declared group, are rejected at load time.
- Files without an `appGroups` field continue to work unchanged — apps render in declaration order with no separators.

## Rules

- **Not every combination needs an activator.** Cells without an activator are simply empty. Only add entries for combinations you want to activate.
- **IDs are case-sensitive.** `"prod"` and `"Prod"` are different ids.
- **Command activators are desktop-only.**
- **Order matters.** Environments, apps, and resources are rendered in the order they appear in the config — put the ones you use most first.

A worked example is included at `springboard-example.json`.
