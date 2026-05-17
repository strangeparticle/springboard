# Editio Domain Provider Boundaries Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move provider-neutral Editio AI/session structures into the core `com.strangeparticle.editio` domain and make OpenAI an adapter that maps to/from provider-specific DTOs for request serialization and response deserialization.

**Architecture:** Core Editio classes model requests, responses, session/history messages, stop reasons, classified errors, and tool-call state without provider package coupling. Provider clients live under `com.strangeparticle.editio.providers`, while OpenAI wire DTOs and mappers live under `com.strangeparticle.editio.providers.openai`. The OpenAI client orchestrates IO only: core request -> OpenAI DTO -> JSON string -> HTTP -> raw JSON string -> OpenAI DTO -> core response or exception.

**Tech Stack:** Kotlin Multiplatform, kotlinx.serialization, Ktor `HttpClient`, Gradle test tasks, kotlin.test.

---

## Scope

This plan covers the Editio AI domain/provider boundary and OpenAI adapter cleanup only.

It does not redesign Springboard tool handlers, tool schemas, chat UI behavior, model selection UI, or persisted app data.

---

## Target Package Structure

Core Editio domain:

- `com.strangeparticle.editio`
- Owns provider-neutral AI/session request and response structures.
- Owns provider-neutral message/history structures.
- Owns provider-neutral stop reasons, classified errors, and exceptions.
- Owns provider-neutral tool-call definitions if those definitions are part of what a model request can use.

Provider boundary:

- `com.strangeparticle.editio.providers`
- Owns provider client interfaces and provider-related metadata.
- Should not own the core request, response, message, error, or session model.

OpenAI adapter:

- `com.strangeparticle.editio.providers.openai`
- Owns OpenAI request DTOs that match request JSON.
- Owns OpenAI response DTOs that match response JSON.
- Owns OpenAI error DTOs that match error JSON.
- Owns mapping between core Editio domain structures and OpenAI DTOs.
- Owns the HTTP client implementation for OpenAI.

---

## File Responsibilities

### Core Domain Files

- Move and rename `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/AiClientRequest.kt` to a core-domain request file under `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/`.
- Move and rename `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/AiClientResponse.kt` to a core-domain response file under `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/`.
- Move and rename `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/AiProviderClientMessage.kt` to a core-domain message file under `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/`.
- Move and rename `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/AiClientStopReason.kt` to a core-domain stop-reason file under `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/`.
- Move and rename `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/AiClientErrorType.kt` to a core-domain error-type file under `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/`.
- Move and rename `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/AiClientException.kt` to a core-domain exception file under `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/`.
- Move `AiProviderClientToolCallDefinition` out of `com.strangeparticle.editio.providers` if it currently lives there, because tool-call definitions are part of the provider-neutral Editio request model.

### Provider Files

- Keep `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/AiClient.kt` under `com.strangeparticle.editio.providers`.
- Keep provider metadata types, such as model info, under `com.strangeparticle.editio.providers` unless they are also clearly core Editio session/domain concepts.

### OpenAI Request Files

- Keep `OpenAiChatCompletionRequest.kt` as the OpenAI request mapping boundary from core request to OpenAI wire DTO.
- Keep existing OpenAI request DTO component files one class per file: `OpenAiMessage.kt`, `OpenAiTool.kt`, `OpenAiToolFunction.kt`, `OpenAiToolCall.kt`, and `OpenAiToolCallFunction.kt`.
- Ensure the OpenAI request path is `core Editio request -> OpenAiChatCompletionRequest -> Json.encodeToString(...)`.
- Do not introduce a generic request-builder abstraction.
- Do not build OpenAI requests through intermediate `JsonObject` values except for existing tool schema payloads.

### OpenAI Response Files

- Add one class per file for OpenAI success response DTOs under `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/openai/`.
- Expected DTO files: `OpenAiChatCompletionResponse.kt`, `OpenAiChoice.kt`, `OpenAiResponseMessage.kt`, `OpenAiResponseToolCall.kt`, and `OpenAiResponseToolCallFunction.kt`.
- These DTOs should mirror OpenAI chat-completions response JSON and use `@SerialName` where the wire field is snake_case.

### OpenAI Error Files

- Add one class per file for OpenAI error DTOs under `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/openai/`.
- Expected DTO files: `OpenAiErrorResponse.kt` and `OpenAiError.kt`.
- These DTOs should mirror the OpenAI error envelope and expose `message`, `type`, and `code` for classification.

### Tests

- Update `composeApp/src/commonTest/kotlin/com/strangeparticle/springboard/app/unit/OpenAiChatCompletionRequestTest.kt`.
- Update `composeApp/src/commonTest/kotlin/com/strangeparticle/springboard/app/unit/OpenAiResponseParserTest.kt`.
- Update any tests that import moved core-domain classes.
- Keep rendered OpenAI request examples in tests as executable assertions.

---

## Task 1: Move Provider-Neutral Domain Classes To Core Editio

**Files:**

- Move/rename current provider-neutral request, response, message, stop-reason, error-type, exception, and tool-definition classes from `com.strangeparticle.editio.providers` to `com.strangeparticle.editio`.
- Modify all imports in `composeApp/src/commonMain/kotlin` and `composeApp/src/commonTest/kotlin`.

**Steps:**

- [ ] Identify all classes in `com.strangeparticle.editio.providers` that are actually provider-neutral Editio domain classes.
- [ ] Move those classes to `com.strangeparticle.editio`.
- [ ] Rename class names that currently include `ProviderClient` if the type is not provider-client-specific.
- [ ] Preserve one class per file.
- [ ] Keep provider-specific message classes, if any, in the provider-specific package instead of moving them into core.
- [ ] Update imports and references throughout main and test sources.
- [ ] Run `./gradlew composeApp:compileKotlinDesktop`.
- [ ] Fix only package/name fallout from this task until desktop compilation passes.

**Expected result:**

- Core Editio request/response/message/error/session model no longer lives under `com.strangeparticle.editio.providers`.
- `com.strangeparticle.editio.providers` contains provider boundary types, not the core domain model.

---

## Task 2: Update Provider Interface To Depend On Core Domain

**Files:**

- Modify `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/AiClient.kt`.
- Modify provider implementations that implement `AiProviderClient`.

**Steps:**

- [ ] Update `AiProviderClient` so `sendAiRequest` accepts the core Editio request type and returns the core Editio response type.
- [ ] Keep `AiProviderClient` itself in `com.strangeparticle.editio.providers`.
- [ ] Keep model-listing provider metadata in the provider package unless a specific type is clearly a core Editio domain concept.
- [ ] Update `AiProviderClientOpenAi` to match the interface signature.
- [ ] Update all call sites.
- [ ] Run `./gradlew composeApp:compileKotlinDesktop`.
- [ ] Fix only interface/call-site fallout from this task until desktop compilation passes.

**Expected result:**

- Provider implementations depend on core Editio domain types.
- Core Editio domain types do not depend on provider implementation packages.

---

## Task 3: Tighten OpenAI Request Mapping And Serialization

**Files:**

- Modify `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/openai/OpenAiChatCompletionRequest.kt`.
- Modify `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/openai/AiClientOpenAi.kt`.
- Modify `composeApp/src/commonTest/kotlin/com/strangeparticle/springboard/app/unit/OpenAiChatCompletionRequestTest.kt`.

**Steps:**

- [ ] Update `OpenAiChatCompletionRequest.from(...)` to accept the renamed core Editio request type.
- [ ] Keep `OpenAiChatCompletionRequest.from(...)` as the only OpenAI request mapping entrypoint.
- [ ] Keep provider-specific message handling in the OpenAI adapter package.
- [ ] Ensure request mapping preserves current OpenAI behavior: system prompt first, OpenAI user messages as `user`, Springboard/current-state messages as user content wrapped in `<current_state>`, assistant tool calls as `assistant` messages with `tool_calls`, and tool results as `tool` messages with `tool_call_id`.
- [ ] Ensure tools are omitted when no tools exist.
- [ ] Ensure `tool_choice` is included as `auto` only when tools exist.
- [ ] Ensure tool-call arguments remain raw JSON strings in the OpenAI request DTO.
- [ ] Update request tests so their helper serializes via `Json.encodeToString(OpenAiChatCompletionRequest.serializer(), OpenAiChatCompletionRequest.from(request))`.
- [ ] Parse the serialized string back to JSON only for structural assertions in tests.
- [ ] Update or add a request test that proves quoted strings and newlines are escaped correctly through actual string serialization.
- [ ] Update `AiProviderClientOpenAi.sendAiRequest` to serialize the OpenAI request DTO directly to a JSON string.
- [ ] Change `postOrThrow` to accept a raw JSON string and pass that string unchanged to `setBody`.
- [ ] Remove request-side `encodeToJsonElement(...).jsonObject` usage from `AiProviderClientOpenAi`.
- [ ] Run `./gradlew composeApp:desktopTest --tests "com.strangeparticle.springboard.app.unit.OpenAiChatCompletionRequestTest"`.

**Expected result:**

- Request generation is domain-to-DTO-to-serialized-string.
- The client does not build request bodies through generic `JsonObject` intermediates.
- Tests prove the exact provider JSON shape through the same serialization path used by production code.

---

## Task 4: Add OpenAI Success Response DTOs

**Files:**

- Create `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/openai/OpenAiChatCompletionResponse.kt`.
- Create `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/openai/OpenAiChoice.kt`.
- Create `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/openai/OpenAiResponseMessage.kt`.
- Create `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/openai/OpenAiResponseToolCall.kt`.
- Create `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/openai/OpenAiResponseToolCallFunction.kt`.

**Steps:**

- [ ] Add OpenAI success response DTOs that mirror the JSON returned by chat completions.
- [ ] Keep each DTO in its own file.
- [ ] Use kotlinx.serialization annotations for snake_case fields such as `finish_reason` and `tool_calls`.
- [ ] Model only fields currently needed for mapping: choices, message content, tool calls, tool-call IDs, function names, function arguments, and finish reason.
- [ ] Rely on existing `Json { ignoreUnknownKeys = true }` behavior for extra provider fields.
- [ ] Run `./gradlew composeApp:compileKotlinDesktop`.

**Expected result:**

- OpenAI success response wire shape is represented by DTOs instead of manual `JsonObject` traversal.

---

## Task 5: Parse OpenAI Success Responses Through DTOs

**Files:**

- Modify `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/openai/OpenAiResponseParser.kt`.
- Modify `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/openai/AiClientOpenAi.kt`.
- Modify `composeApp/src/commonTest/kotlin/com/strangeparticle/springboard/app/unit/OpenAiResponseParserTest.kt`.

**Steps:**

- [ ] Change `OpenAiResponseParser.parseSuccess` to accept the raw response JSON string.
- [ ] Decode the raw string into `OpenAiChatCompletionResponse` using kotlinx.serialization.
- [ ] Map the decoded DTO to the core Editio response type.
- [ ] Preserve the existing rule that only the first choice is consumed.
- [ ] Preserve malformed-response classification for missing or empty choices.
- [ ] Preserve malformed-response classification for missing required tool-call fields.
- [ ] Preserve validation that OpenAI tool-call function arguments are valid JSON object strings before constructing the core `ToolCall`.
- [ ] Preserve stop-reason mapping: `stop`, `tool_calls`, `length`, and fallback to `Other`.
- [ ] Preserve the response `raw` field by parsing the raw response string into a `JsonObject` after or before DTO decode.
- [ ] Update success parser tests to pass raw JSON strings into `parseSuccess`.
- [ ] Run `./gradlew composeApp:desktopTest --tests "com.strangeparticle.springboard.app.unit.OpenAiResponseParserTest"`.

**Expected result:**

- Success response handling follows `raw JSON string -> OpenAI DTO -> core Editio response`.
- Manual `JsonObject` traversal is removed from normal success response mapping.

---

## Task 6: Add OpenAI Error DTOs And DTO-Based Error Classification

**Files:**

- Create `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/openai/OpenAiErrorResponse.kt`.
- Create `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/openai/OpenAiError.kt`.
- Modify `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/openai/OpenAiResponseParser.kt`.
- Modify `composeApp/src/commonTest/kotlin/com/strangeparticle/springboard/app/unit/OpenAiResponseParserTest.kt`.

**Steps:**

- [ ] Add DTOs for OpenAI error envelope and error payload.
- [ ] Decode non-2xx raw response bodies into the OpenAI error DTO when possible.
- [ ] Classify by OpenAI `error.code` first.
- [ ] Classify by OpenAI `error.type` second where the type is specific enough.
- [ ] Fall back to HTTP-status classification when the body is non-JSON, does not match the DTO, or has unrecognized code/type values.
- [ ] Preserve current mappings for `context_length_exceeded`, `insufficient_quota`, `rate_limit_exceeded`, `invalid_api_key`, and `invalid_token`.
- [ ] Preserve current status fallback mappings for 401/403, 429, 5xx, and unknown statuses.
- [ ] Preserve raw provider message behavior: use structured error message when available, otherwise use raw body.
- [ ] Add or update tests for `context_length_exceeded`, `insufficient_quota`, `rate_limit_exceeded`, `invalid_api_key`, unrecognized code fallback, and non-JSON fallback.
- [ ] Run `./gradlew composeApp:desktopTest --tests "com.strangeparticle.springboard.app.unit.OpenAiResponseParserTest"`.

**Expected result:**

- Error response handling follows `raw JSON string -> OpenAI error DTO -> core Editio exception` when possible.
- HTTP fallback behavior remains intact.

---

## Task 7: Clean Up OpenAI Client IO Boundary

**Files:**

- Modify `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/providers/openai/AiClientOpenAi.kt`.

**Steps:**

- [ ] Ensure `sendAiRequest` performs only orchestration: API key lookup, request DTO serialization, POST, raw response extraction, success/error delegation.
- [ ] Remove chat-completion `parseJsonOrThrow` usage if success parsing now accepts raw strings.
- [ ] Keep model-list parsing behavior unchanged unless compile requires import/name updates.
- [ ] Ensure cancellation still propagates and network exceptions still map to the core network error type.
- [ ] Remove unused imports introduced by prior `JsonObject` request/response handling.
- [ ] Run `./gradlew composeApp:compileKotlinDesktop`.

**Expected result:**

- `AiProviderClientOpenAi` is an adapter/orchestrator, not a mapper or parser.
- Mapping and parsing live in OpenAI DTO/parser code.

---

## Task 8: Update Remaining Imports And Tests Across The App

**Files:**

- Modify all source and test files with stale imports from moved core-domain types.

**Steps:**

- [ ] Search for `com.strangeparticle.editio.providers.AiProviderClientRequest` and replace with the new core type import.
- [ ] Search for `com.strangeparticle.editio.providers.AiProviderClientResponse` and replace with the new core type import.
- [ ] Search for `com.strangeparticle.editio.providers.AiProviderClientMessage` and replace with the new core type import.
- [ ] Search for `com.strangeparticle.editio.providers.AiProviderClientStopReason` and replace with the new core type import.
- [ ] Search for `com.strangeparticle.editio.providers.AiProviderClientErrorType` and replace with the new core type import.
- [ ] Search for `com.strangeparticle.editio.providers.AiProviderClientException` and replace with the new core type import.
- [ ] Search for `AiProviderClientToolCallDefinition` and replace with the new core type name/import if renamed.
- [ ] Run `./gradlew composeApp:compileKotlinDesktop`.
- [ ] Run focused OpenAI tests.

**Expected result:**

- No stale references remain to provider-packaged core domain classes.
- Focused OpenAI request/response tests pass.

---

## Task 9: Full Verification

**Files:**

- No intended source changes except fixes required by verification failures.

**Steps:**

- [ ] Run `./gradlew composeApp:desktopTest --tests "com.strangeparticle.springboard.app.unit.OpenAi*"`.
- [ ] Fix any focused OpenAI failures while preserving the domain/provider boundary.
- [ ] Run `./gradlew composeApp:allTests`.
- [ ] Fix any full-suite failures caused by the package/domain rename or DTO mapping changes.
- [ ] Run `git diff --stat` and inspect the changed-file list for accidental unrelated edits.

**Expected result:**

- Focused OpenAI tests pass.
- Full test suite passes.
- Changes are limited to the Editio domain/provider boundary and OpenAI adapter cleanup.

---

## Verification Commands

Use these commands during execution:

- `./gradlew composeApp:compileKotlinDesktop`
- `./gradlew composeApp:desktopTest --tests "com.strangeparticle.springboard.app.unit.OpenAiChatCompletionRequestTest"`
- `./gradlew composeApp:desktopTest --tests "com.strangeparticle.springboard.app.unit.OpenAiResponseParserTest"`
- `./gradlew composeApp:desktopTest --tests "com.strangeparticle.springboard.app.unit.OpenAi*"`
- `./gradlew composeApp:allTests`

---

## Self-Review Notes

- The plan starts with the core Editio domain package boundary before OpenAI request/response work.
- The OpenAI request path is explicitly domain-to-DTO-to-serialized-string.
- The OpenAI response path is explicitly raw-string-to-DTO-to-domain-response.
- The OpenAI error path is explicitly raw-string-to-error-DTO-to-domain-exception with HTTP fallback.
- The plan avoids a generic request-builder abstraction.
- The plan avoids embedding generated Kotlin implementations; it documents responsibilities, sequencing, and expected behavior for an implementing agent.
