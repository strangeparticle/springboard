# AI Editing in Springboard

Springboard's AI editing feature lets users describe changes to their springboard configurations in natural language. The AI executes those changes directly using native tool calls — no copy/paste, no manual JSON edits. The chat session is global across all open tabs; tool calls that operate on a specific tab identify it by `tab_id`.


This document covers Springboard's implementation and integration of the Editio tool-call framework. For Editio's architecture and how to add new providers, see `README-Editio.md`.

---

## How It Fits Together

TODO: editio and its role in this call chain (below) should be better documented, as should the seams between editio and springboard

```
User types a message
        ↓
AiSessionManager (not yet implemented — Tasks 27+)
        ↓ AiClientRequest (history + tools + system prompt)
AiProviderClientOpenAi
        ↓ AiClientResponse (text + toolCalls)
ToolCallDispatcher
        ↓ per-tool executeToolCallHandler(toolCallId, argsJson, context)
SpringboardToolCallHandler  (one of ~30 handlers)
        ↓ decodeToolCallHandlerRequest → validates → domain mutator
SpringboardViewModel.replaceTabSpringboard(tabId, updatedSpringboard)
        ↓
SpringboardAppSnapshot.capture(viewModel) → JSON → back to model as tool result
```

---

## Springboard-Specific Editio Types

These live in `com.strangeparticle.springboard.app.editio.*`. They extend Editio's marker interfaces with Springboard's concrete runtime needs.

### SpringboardAppSnapshot

`SpringboardAppSnapshot` is a point-in-time picture of all open tabs and their springboard state. It serves three purposes:

1. **Tool result payload** — every successful mutation returns a snapshot so the model always sees the current state after it acts.
2. **State injection** — when `stateChangedSinceLastSnapshotSent` is true, a fresh snapshot is prepended to the request history (as `AiSystemStateMessage`) before calling the provider.
3. **Initial context** — the first request always includes a snapshot even if no tool calls have been made.

```kotlin
@Serializable
data class SpringboardAppSnapshot(
    val tabs: List<SpringboardTabSnapshot>,
    val activeTabId: String?,
)

@Serializable
data class SpringboardTabSnapshot(
    val tabId: String,
    val label: String,
    val source: String?,
    val isDirty: Boolean,
    val springboard: SpringboardDto?,   // full springboard data, null for empty tabs
)
```

The snapshot is serialized compact (no whitespace) via `toCompactJson()` and parsed back via `fromJson(jsonString)`. Both are on the companion object.

### SpringboardToolCallExecutionContext

Extends `ToolCallExecutionContext` with Springboard's runtime dependencies:

```kotlin
interface SpringboardToolCallExecutionContext : ToolCallExecutionContext {
    val viewModel: SpringboardViewModel

    fun markStateChanged()
    // Signals that the next outgoing request needs a fresh snapshot injected.
    // Call after every successful mutation. Do NOT call for read-only tools.

    suspend fun awaitUserApproval(toolCallId: String): Boolean
    // Suspends until the user clicks Apply (true) or Cancel (false)
    // in the inline confirmation card in the chat pane.
}
```

Convenience helpers on the context (defined as extension functions):

| Helper | When to use |
|---|---|
| `successResult()` | Mutation succeeded; returns snapshot |
| `errorResult(message, code)` | Validation or precondition failure |
| `errorResult(SpringboardMutationError)` | Propagate a structured mutator error |
| `successStatusResult(message?)` | Non-mutation success (no snapshot) |
| `errorStatusResult(message, code)` | Non-mutation failure (no snapshot) |
| `getSpringboardForTabOrError(tabId)` | Resolve tab → springboard or throw |
| `applyMutation(tabId) { mutator() }` | Apply, mark dirty, mark state changed, return success |
| `handleMutationErrors { block }` | Catch `SpringboardMutationError` → `errorResult` |

Use `successStatusResult` (no snapshot) for tools that don't mutate state, such as `respond_with_message` and `save_springboard`. Use `successResult` (with snapshot) for tools that mutate the springboard.

### SpringboardToolCallHandlerResponse

```kotlin
@Serializable
data class SpringboardToolCallHandlerResponse(
    val success: Boolean,
    val message: String? = null,
    val code: String? = null,
    val state: SpringboardAppSnapshot? = null,  // null for non-mutation results
) : ToolCallHandlerResponse
```

---

## Domain Mutators

All springboard mutations are pure functions in `com.strangeparticle.springboard.app.domain.mutator`. They take a `Springboard` and return an updated `Springboard`. They never touch the viewmodel, coroutines, or UI. Validation failures throw `SpringboardMutationError(errorMessage, code)`.

```kotlin
// Example:
fun addApp(springboard: Springboard, app: App): Springboard { … }
fun updateApp(springboard: Springboard, appId: String, newName: String? = null, …): Springboard { … }
fun removeApp(springboard: Springboard, appId: String): Springboard { … }
```

The pattern in a tool handler:

```kotlin
context.applyMutation(args.tab_id) {
    addApp(springboard, App(id = args.id, name = args.name))
}
```

`applyMutation` calls the lambda, catches any `SpringboardMutationError`, and on success calls `viewModel.replaceTabSpringboard`, `viewModel.markTabDirty`, and `markStateChanged` before returning `successResult()`.

### Validation Policy by Operation

| Operation | Rules |
|---|---|
| **add** | id must not be blank; id must not already exist; referenced ids (appGroupId, coordinate components) must exist |
| **update** | target id must exist; `app_group_id` and `clear_app_group_id` are mutually exclusive |
| **remove** | target id must exist; refuses if other entities reference it (cascade-by-refusal) |
| **reorder** | supplied id list must be a complete permutation of current ids (no extras, no omissions) |
| **add guidance** | coordinate components must exist AND an activator must exist at that coordinate; `guidance_lines` must not be empty |
| **update guidance** | `guidance_lines` must not be empty |

Errors always carry a machine-readable `code` string for model introspection:

| Code | Meaning |
|---|---|
| `blank_id` | Entity id is blank |
| `duplicate_id` | Id already exists |
| `missing_target` | Id to update/remove not found |
| `missing_reference` | Referenced id (e.g. appGroupId) not found |
| `in_use` | Cannot remove — other entities reference this id |
| `reserved_id` | Id matches a reserved value (e.g. `ALL` for environments) |
| `conflicting_fields` | Mutually exclusive fields both set |
| `missing_activator` | Guidance requires an activator at the same coordinate |
| `empty_guidance_lines` | guidance_lines list is empty |
| `missing_tab` | tab_id doesn't match any open tab |
| `tab_empty` | Tab has no loaded springboard |
| `not_supported_for_source` | Operation not valid for this tab's source type |

---

## Tool Surface

All tools are in `com.strangeparticle.springboard.app.editio.toolcall.*`. Each is a `ToolCallHandler` + a `@Serializable` request DTO.

### Entity CRUD

All entity-type tools follow: `tab_id` (which springboard), entity fields, `display_message` (user-visible summary).

| Tool | Args |
|---|---|
| `add_app` | `tab_id`, `id`, `name`, `app_group_id?` |
| `update_app` | `tab_id`, `id`, `name?`, `app_group_id?`, `clear_app_group_id` |
| `remove_app` | `tab_id`, `id` |
| `add_resource` | `tab_id`, `id`, `name` |
| `update_resource` | `tab_id`, `id`, `name?` |
| `remove_resource` | `tab_id`, `id` |
| `add_environment` | `tab_id`, `id`, `name` |
| `update_environment` | `tab_id`, `id`, `name?` |
| `remove_environment` | `tab_id`, `id` |
| `add_app_group` | `tab_id`, `id`, `description?` |
| `update_app_group` | `tab_id`, `id`, `description?` |
| `remove_app_group` | `tab_id`, `id` |

### Activators

Activators are addressed by coordinate: `(environment_id, app_id, resource_id)`. Use `ALL` as `environment_id` to create an all-environments activator.

| Tool | Args |
|---|---|
| `add_url_activator` | `tab_id`, `app_id`, `resource_id`, `environment_id`, `url` |
| `add_url_template_activator` | `tab_id`, `app_id`, `resource_id`, `environment_id`, `url_template` |
| `add_command_activator` | `tab_id`, `app_id`, `resource_id`, `environment_id`, `command_template` |
| `update_activator` | `tab_id`, `app_id`, `resource_id`, `environment_id`, `url?`/`url_template?`/`command_template?` |
| `remove_activator` | `tab_id`, `app_id`, `resource_id`, `environment_id` |
| `move_activator` | `from_tab_id`, `to_tab_id`, `app_id`, `resource_id`, `environment_id` |

`update_activator` is type-aware: only the field that matches the activator's current type is accepted. To change types, remove and re-add.

`move_activator` is atomic: it validates the destination before touching the source. If the add fails (e.g. coordinate already occupied in the destination), the source is left untouched.

### Guidance

Guidance is attached to activator coordinates — a guidance entry can only exist if an activator exists at the same coordinate.

| Tool | Args |
|---|---|
| `add_guidance` | `tab_id`, `app_id`, `resource_id`, `environment_id`, `guidance_lines: List<String>` |
| `update_guidance` | `tab_id`, `app_id`, `resource_id`, `environment_id`, `guidance_lines: List<String>` |
| `remove_guidance` | `tab_id`, `app_id`, `resource_id`, `environment_id` |

### Reordering

Each reorder tool takes `tab_id` and `ordered_ids: List<String>` containing the complete new ordering of every existing item. Partial lists are rejected.

`reorder_apps`, `reorder_resources`, `reorder_environments`, `reorder_app_groups`, `reorder_activators` (for activators, `ordered_coordinates: List<{env_id, app_id, resource_id}>`).

### Tab Management

| Tool | Behavior |
|---|---|
| `create_tab` | Creates a new empty tab and makes it active |
| `close_tab` | Closes a tab; refuses if the tab is dirty |
| `open_local_file` | Loads a local file path; `in_new_tab: Boolean` |
| `open_from_url` | Loads an HTTP/S3 URL; `in_new_tab: Boolean` |

### Special Tools

| Tool | Behavior |
|---|---|
| `respond_with_message` | Returns a prose reply without mutating state. Used for educational answers ("what is an activator?") and declines. Returns no snapshot. |
| `save_springboard` | Saves a local-file-backed tab to disk. **Confirmation-gated** — see below. |

---

## Confirmation-Gated Tools

`save_springboard` is the only tool with `requiresUserConfirmation = true`. Its `executeToolCallHandler` calls `context.awaitUserApproval(toolCallId)` after validating the source type but before writing to disk. This suspends the coroutine until the user clicks Apply or Cancel in the inline confirmation card rendered in the chat transcript.

Flow:
1. Model requests `save_springboard(tab_id = "tab-1")`.
2. Dispatcher calls `executeToolCallHandler("call-xyz", …, context)`.
3. Handler validates source is a local file path.
4. Handler calls `context.awaitUserApproval("call-xyz")` — suspends.
5. Session manager adds `ToolCall(state=ApprovalRequested)` part to the chat transcript.
6. UI renders an inline card with Apply / Cancel buttons.
7. User clicks → `SpringboardToolCallExecutionContext` resolves the deferred.
8. Handler resumes, calls `viewModel.saveTab(args.tab_id)`, returns result.

The `toolCallId` forwarded by the dispatcher is used as the key for the pending deferred, so the UI can match the button click to the right suspended handler.

---

## File Map

```
composeApp/src/commonMain/kotlin/
├── com/strangeparticle/editio/                          ← Editio core (provider-neutral)
│   ├── Ai*.kt                                           ← message, request, response, error types
│   ├── providers/
│   │   ├── AiClient.kt
│   │   ├── AiClientModelInfo.kt
│   │   └── openai/                                      ← OpenAI impl
│   └── toolcall/
│       ├── ToolCall*.kt                                 ← framework types
│       └── ToolCallSchema.kt                            ← requestSchema, @ToolFieldDescription
│
└── com/strangeparticle/springboard/app/
    ├── editio/                                          ← Springboard's Editio integration
    │   ├── SpringboardAppSnapshot.kt
    │   ├── SpringboardTabSnapshot.kt
    │   ├── SpringboardToolCallExecutionContext.kt
    │   ├── SpringboardToolCallHandlerResponse.kt
    │   └── toolcall/                                    ← all 30+ tool handlers + request DTOs
    │       ├── AddAppToolCallHandler.kt
    │       ├── AddAppToolCallHandlerRequest.kt
    │       └── … (one *Handler.kt + *HandlerRequest.kt per tool)
    │
    ├── domain/
    │   ├── factory/
    │   │   ├── SpringboardDtoMapper.kt                  ← Springboard ↔ SpringboardDto mapping
    │   │   └── SpringboardJsonWriter.kt                 ← serialize to JSON for save
    │   └── mutator/
    │       ├── AppMutator.kt
    │       ├── ResourceMutator.kt
    │       ├── EnvironmentMutator.kt
    │       ├── AppGroupMutator.kt
    │       ├── ActivatorMutator.kt
    │       ├── GuidanceMutator.kt
    │       ├── ReorderMutator.kt
    │       └── SpringboardMutationError.kt
    │
    └── viewmodel/
        └── SpringboardViewModel.kt                      ← saveTab(tabId), replaceTabSpringboard, markTabDirty
```

---

## Testing

Test files live in `composeApp/src/commonTest/kotlin/…/unit/tools/` and `…/shared/`.

**`SpringboardToolCallExecutionContextInMemoryFake`** — the test double for `SpringboardToolCallExecutionContext`. Wraps a real `SpringboardViewModel` so mutations actually apply. Exposes:
- `stateChangedCount: Int` — how many times `markStateChanged()` was called.
- `pendingApprovals: MutableMap<String, CompletableDeferred<Boolean>>` — for driving confirmation-gated tools.
- `resolveApproval(toolCallId, approved)` — resolves the deferred from the test.

**`AiProviderClientInMemoryFake`** — the test double for `AiProviderClient`. Returns scripted responses from a queue; records all calls for assertion.

Run targeted test groups:

```bash
./gradlew :composeApp:desktopTest --tests "*EntityCrudTools*"
./gradlew :composeApp:desktopTest --tests "*ActivatorTools*"
./gradlew :composeApp:desktopTest --tests "*GuidanceTools*"
./gradlew :composeApp:desktopTest --tests "*ReorderTools*"
./gradlew :composeApp:desktopTest --tests "*TabManagement*"
./gradlew :composeApp:desktopTest --tests "*MoveActivator*"
./gradlew :composeApp:desktopTest --tests "*SaveSpringboard*"
./gradlew :composeApp:desktopTest --tests "*ToolCallExecutor*"
```

Full suite: `./gradlew :composeApp:desktopTest`

---

## What Is Not Yet Implemented (as of Tasks 1–26)

The session manager, system prompt builder, chat pane UI, and AI settings screen are in Tasks 27–42. The items below are not yet wired up:

- `AiSessionManager` — drives the agent loop, injects snapshots, handles cancellation and token-budget eviction.
- `SystemPromptBuilder` — static preamble + behavioral rules, regenerated fresh on each request.
- `AiChatPane` — bottom-docked chat UI, transcript rendering, Stop button, inline approval cards.
- AI settings screen — provider selector, API key field, model dropdown with fetch-from-API.
- `ToolCallRegistry` population at startup — handlers are implemented but not yet registered anywhere.
