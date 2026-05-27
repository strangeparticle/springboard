# Luther — AI Tool-Call Framework

Luther is a provider-neutral AI tool-call framework written in Kotlin Multiplatform. It handles the plumbing between an AI model and a host application: translating provider-specific wire formats into a clean set of domain types, routing model-requested tool calls to typed handler implementations, and isolating everything provider-specific behind a single interface.

**Intended future state:** Luther is designed to be extracted into a standalone KMP library. All Luther code lives under `com.strangeparticle.luther.*`; it has no dependency on Springboard domain types. Host-application integration is done entirely through marker interfaces (`ToolCallExecutionContext`, `ToolCallHandlerResponse`) that the host implements.

---

## Package Layout

```
com.strangeparticle.luther
├── AiClientMessage           ← base conversation message type (open class)
├── AiUserMessage             ← user turn
├── AiAssistantMessage        ← model turn (text + optional tool calls)
├── AiSystemStateMessage      ← synthetic state-injection turn
├── AiClientRequest           ← one provider turn (model, prompt, history, tools)
├── AiClientResponse          ← provider's reply (text, toolCalls, stopReason, raw JSON)
├── AiClientStopReason        ← Stop | ToolUse | MaxTokens | Other
├── AiClientErrorType         ← classified error enum
├── AiClientException         ← runtime exception carrying AiClientErrorType
├── AiToolCallDefinition      ← provider-neutral tool description (name, desc, JSON schema)
│
├── providers/
│   ├── AiProviderClient          ← interface every provider implements
│   ├── AiProviderClientModelInfo ← model metadata for the settings dropdown
│   └── openai/
│       ├── AiProviderClientOpenAi        ← Ktor-based OpenAI impl
│       ├── OpenAiChatCompletionRequest   ← @Serializable request DTO
│       ├── OpenAiChatCompletionResponse  ← @Serializable response DTO
│       ├── OpenAiResponseParser          ← response body → AiClientResponse
│       ├── OpenAiModelFilter             ← keeps chat-completion-capable models
│       └── … supporting DTO files
│
└── toolcall/
    ├── ToolCall                      ← a single model-requested invocation
    ├── ToolCallHandler               ← interface every tool implements
    ├── ToolCallHandlerResponse       ← marker interface for handler results
    ├── ToolCallExecutionContext      ← marker interface for handler dependencies
    ├── ToolCallExecutionResult       ← generic dispatcher-level error result
    ├── ToolCallRegistry              ← holds registered handlers
    ├── ToolCallDispatcher            ← routes ToolCalls to handlers
    ├── ToolCallProviderClientMessage ← "tool" role history message
    ├── ToolCallSchema                ← annotation-driven JSON Schema generator
    └── ToolCallHandlerUtil           ← decodeToolCallHandlerRequest helper
```

---

## Core Types

### Conversation History

`AiClientMessage` is an open class. The four concrete message types are:

| Type | Role in provider | Purpose |
|---|---|---|
| `AiUserMessage(text)` | `"user"` | User's typed message |
| `AiAssistantMessage(text?, toolCalls)` | `"assistant"` | Model's reply, may request tool calls |
| `ToolCallProviderClientMessage(toolCallId, content)` | `"tool"` | Result of a tool call, returned to the model |
| `AiSystemStateMessage(snapshotJson)` | `"user"` wrapped in `<current_state>…</current_state>` | Synthetic state injection |

`AiSystemStateMessage` is sent as a `user`-role message (not `system`) because OpenAI's tool-calling API does not accept tool-result content inside a `system` message. The wrapping tag keeps it clearly machine-authored.

### Request and Response

```kotlin
data class AiClientRequest(
    val modelId: String,
    val systemPrompt: String,
    val history: List<AiClientMessage>,
    val tools: List<AiToolCallDefinition>,
)

data class AiClientResponse(
    val text: String?,
    val toolCalls: List<ToolCall>,
    val stopReason: AiClientStopReason,
    val raw: JsonObject,               // full provider body for debugging
)
```

### Error Classification

`AiClientException` wraps every provider failure with a classified `AiClientErrorType`:

| Error class | When |
|---|---|
| `InvalidApiKey` | 401 / 403, or key is blank/missing |
| `RateLimit` | 429 |
| `QuotaExceeded` | 402 / specific quota errors |
| `ContextTooLarge` | Model context-window exceeded |
| `Network` | Transport-layer exception (no HTTP response) |
| `ProviderUnavailable` | 5xx |
| `MalformedResponse` | JSON parse failure on a success status |
| `Unknown` | Anything else |

---

## Provider Interface

```kotlin
internal interface AiProviderClient {
    suspend fun sendAiRequest(request: AiClientRequest): AiClientResponse
    suspend fun listModels(apiKey: String): List<AiProviderClientModelInfo>
}
```

`sendAiRequest` does one model turn. It never loops — the caller drives the agent loop by calling it repeatedly until `stopReason == Stop`. `listModels` is used by the settings screen to populate the model dropdown after the user enters a key.

### Implementing a New Provider

1. Create a new package under `com.strangeparticle.luther.providers.<name>/`.
2. Implement `AiProviderClient`.
3. Map `AiClientRequest` → the provider's native request format using `@Serializable` DTOs. Use `Json.encodeToString(Dto.serializer(), dto)` for the body.
4. POST via Ktor `HttpClient`. On non-200, call `parseErrorAndThrow(httpStatus, bodyText)` — a pure function that maps status codes to `AiClientException`.
5. Parse the success body through `@Serializable` response DTOs. Map:
   - `choices[0].message.content` → `AiClientResponse.text`
   - `choices[0].message.tool_calls` → `List<ToolCall>`
   - `finish_reason` → `AiClientStopReason`
6. Propagate `CancellationException` without wrapping it (cooperative cancellation must not be swallowed).

The OpenAI implementation (`AiProviderClientOpenAi`) is the reference; `OpenAiChatCompletionRequest.from(request)` shows how to map the full message history including all four message types.

---

## Tool Call Framework

### Key Types

```kotlin
data class ToolCall(
    val toolCallId: String,
    val toolName: String,
    val argumentsAsJsonString: String,
)

data class AiToolCallDefinition(
    val name: String,
    val description: String,
    val schema: JsonObject,         // JSON Schema for the arguments object
)
```

### ToolCallHandler

Every tool implements `ToolCallHandler`:

```kotlin
interface ToolCallHandler {
    val providerToolId: String          // wire name, e.g. "add_app"
    val description: String             // shown to the model
    val schema: JsonObject              // JSON Schema for the args
    val requiresUserConfirmation: Boolean get() = false

    suspend fun executeToolCallHandler(
        toolCallId: String,             // dispatcher-forwarded ID
        argumentsAsJsonString: String,  // raw JSON from the model
        context: ToolCallExecutionContext,
    ): ToolCallHandlerResponse
}
```

`ToolCallExecutionContext` and `ToolCallHandlerResponse` are both marker interfaces. The host application defines concrete subtypes carrying its own runtime dependencies and result structure.

### Schema Generation

Use `requestSchema(serializer)` to derive the JSON Schema automatically from a `@Serializable` data class. Field descriptions come from `@ToolFieldDescription` annotations:

```kotlin
@Serializable
data class AddAppToolCallHandlerRequest(
    @ToolFieldDescription("Id of the tab whose springboard to mutate.")
    val tab_id: String,
    @ToolFieldDescription("Stable id for the new app.")
    val id: String,
    @ToolFieldDescription("Display name for the new app.")
    val name: String,
    @ToolFieldDescription("Optional id of an existing app group to assign this app to.")
    val app_group_id: String? = null,
    @ToolFieldDescription("Brief user-facing description of what was done.")
    val display_message: String,
)

// In the handler:
override val schema = requestSchema(AddAppToolCallHandlerRequest.serializer())
```

`requestSchema` reflects over the `SerialDescriptor` tree and produces a `{"type":"object", "properties":…, "required":[…]}` JSON Schema. Optional (`= null`) fields are excluded from `required`. Supported kinds: `String`, `Boolean`, integer types, `Float`/`Double`, enums, lists, and nested objects.

Use `decodeToolCallHandlerRequest(argumentsAsJsonString, serializer)` inside a handler to parse the raw JSON string into the typed request:

```kotlin
val args = decodeToolCallHandlerRequest(argumentsAsJsonString, AddAppToolCallHandlerRequest.serializer())
```

### Registry and Dispatcher

```kotlin
// Registration (at app startup):
val registry = ToolCallRegistry()
registry.register(AddAppToolCallHandler())
registry.register(SaveSpringboardToolCallHandler())
// …

// Definitions sent to the model on every request:
val toolDefinitions: List<AiToolCallDefinition> = registry.getDefinitions()

// Dispatching (in the agent loop, for each ToolCall in AiClientResponse.toolCalls):
val dispatcher = ToolCallDispatcher(registry)
val handlerResponse: ToolCallHandlerResponse = dispatcher.execute(
    toolCallId = toolCall.toolCallId,
    providerToolId = toolCall.toolName,
    argumentsAsJsonString = toolCall.argumentsAsJsonString,
    context = myContext,
)
```

`ToolCallDispatcher.execute` catches `SerializationException` from argument decoding and returns a `ToolCallExecutionResult(success=false, code="invalid_arguments")` rather than propagating it — the model self-corrects on retry. Unknown tool names return `code="unknown_tool"`. All other exceptions propagate to the caller.

### Implementing a Tool Call Handler

1. Create a `@Serializable` data class for the arguments (name it `<ToolName>ToolCallHandlerRequest`).
2. Annotate every field with `@ToolFieldDescription`.
3. Create a class implementing `ToolCallHandler`:
   - `providerToolId` = the snake_case name the model will use.
   - `schema = requestSchema(MyRequest.serializer())`.
   - Implement `executeToolCallHandler(toolCallId, argumentsAsJsonString, context)`:
     - Parse args with `decodeToolCallHandlerRequest(argumentsAsJsonString, MyRequest.serializer())`.
     - Cast `context` to your host-specific context type.
     - Perform work. Return a host-specific `ToolCallHandlerResponse`.
     - Do NOT throw on domain validation failures — return a structured error response.
     - Let `SerializationException` propagate (dispatcher handles it).
4. Register the handler in `ToolCallRegistry` at startup.

### Confirmation-Gated Tools

Set `override val requiresUserConfirmation = true`. In `executeToolCallHandler`, call the host's approval mechanism (e.g. `context.awaitUserApproval(toolCallId)`) before performing the action. The `toolCallId` is forwarded by the dispatcher so the UI can tie the approval widget to the correct pending call.

---

## OpenAI Provider Notes

The OpenAI provider (`AiProviderClientOpenAi`) serializes requests via `OpenAiChatCompletionRequest.from(request)`:

- System prompt → `{"role":"system", "content":…}` as the first message.
- `AiSystemStateMessage` → `{"role":"user", "content":"<current_state>{json}</current_state>"}`.
- `AiAssistantMessage` with tool calls → `"tool_calls"` array in the assistant message.
- `ToolCallProviderClientMessage` → `{"role":"tool", "tool_call_id":…, "content":…}`.

Tools are sent as `{"type":"function", "function": {"name":…, "description":…, "parameters":<schema>}}`. When tools are present, `tool_choice` is set to `"auto"`.

`OpenAiModelFilter` keeps models whose id starts with `gpt-` and does not match any of: `embedding`, `moderation`, `whisper`, `tts`, `dall-e`, `babbage`, `davinci`, `audio` (case-insensitive substring match).
