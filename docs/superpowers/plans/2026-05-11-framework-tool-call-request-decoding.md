# Framework Tool Call Request Decoding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove per-request `fromJsonString(...)` wrappers and have handlers call the framework decoder directly.

**Architecture:** `decodeToolCallHandlerRequest(...)` remains the single framework-owned JSON decoding entry point in `editio.toolcall`. Springboard request DTOs stay as serializable data classes only; Springboard handlers decode raw arguments by passing the relevant serializer to the framework helper.

**Tech Stack:** Kotlin Multiplatform, kotlinx.serialization, Gradle desktop/common tests.

---

### Task 1: Route Handlers Through Framework Decoder

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/strangeparticle/springboard/app/editio/toolcall/*ToolCallHandler.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/strangeparticle/springboard/app/editio/toolcall/*ToolCallHandlerRequest.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/strangeparticle/springboard/app/unit/ToolCallRegistryTest.kt`

- [ ] **Step 1: Create failing compile state**

Update one handler to use `decodeToolCallHandlerRequest(argumentsAsJsonString, Request.serializer())` directly and remove that request class's `fromJsonString(...)` companion. Run the focused tool-call tests and expect compile failures anywhere the removed wrapper is still referenced.

- [ ] **Step 2: Update all handlers**

For every handler, replace `SomeToolCallHandlerRequest.fromJsonString(argumentsAsJsonString)` with:

```kotlin
decodeToolCallHandlerRequest(argumentsAsJsonString, SomeToolCallHandlerRequest.serializer())
```

Add `import com.strangeparticle.editio.toolcall.decodeToolCallHandlerRequest` to each handler file that needs it.

- [ ] **Step 3: Strip request DTO companions**

For every request DTO that only delegates to the framework decoder, remove the `decodeToolCallHandlerRequest` import and the `companion object`.

- [ ] **Step 4: Verify no wrappers remain**

Run a search for `fromJsonString(` under `composeApp/src/commonMain/kotlin/com/strangeparticle/springboard/app/editio/toolcall` and expect no matches.

- [ ] **Step 5: Verify focused tests**

Run: `./gradlew composeApp:desktopTest --tests "com.strangeparticle.springboard.app.unit.ToolCallExecutorTest" --tests "com.strangeparticle.springboard.app.unit.ToolCallRegistryTest" --tests "com.strangeparticle.springboard.app.unit.tools.*"`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Verify full suite**

Run: `./gradlew composeApp:allTests`

Expected: `BUILD SUCCESSFUL`.
