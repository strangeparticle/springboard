# Generic Tool Call Dispatcher Errors Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move generic dispatcher error responses into `editio.toolcall` and remove the Springboard-specific error-handler interface.

**Architecture:** `ToolCallDispatcher` should own generic execution failures for unknown tools and invalid argument JSON. Springboard tools continue returning `SpringboardToolCallHandlerResponse` when they need Springboard state, while generic dispatcher failures return a provider-neutral `ToolCallExecutionResult`.

**Tech Stack:** Kotlin Multiplatform, kotlinx.serialization, Gradle desktop/common tests.

---

### Task 1: Add Generic Dispatcher Error Result

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/toolcall/ToolCallExecutionResult.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/strangeparticle/springboard/app/unit/ToolCallExecutorTest.kt`

- [ ] **Step 1: Write the failing test**

Update dispatcher tests to stop injecting `ToolCallExecutionErrorHandler` and assert `ToolCallExecutionResult` for unknown tools and invalid arguments:

```kotlin
assertEquals(
    ToolCallExecutionResult(
        success = false,
        message = "Unknown tool: 'missing_tool'",
        code = "unknown_tool",
    ),
    result,
)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew composeApp:desktopTest --tests "com.strangeparticle.springboard.app.unit.ToolCallExecutorTest"`

Expected: compile failure because `ToolCallExecutionResult` does not exist and `ToolCallDispatcher` still requires an error handler.

- [ ] **Step 3: Write minimal implementation**

Create `ToolCallExecutionResult`:

```kotlin
package com.strangeparticle.editio.toolcall

import kotlinx.serialization.Serializable

@Serializable
internal data class ToolCallExecutionResult(
    val success: Boolean,
    val message: String? = null,
    val code: String? = null,
) : ToolCallHandlerResponse
```

- [ ] **Step 4: Run test to verify next failure**

Run the focused test again. Expected: compile failure around dispatcher constructor or obsolete test error handler.

### Task 2: Remove Error Handler Interface

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/toolcall/ToolCallDispatcher.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/strangeparticle/editio/toolcall/ToolCallExecutionErrorHandler.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/strangeparticle/springboard/app/editio/SpringboardToolCallExecutionErrorHandler.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/strangeparticle/springboard/app/unit/ToolCallExecutorTest.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/strangeparticle/springboard/app/unit/ToolCallRegistryTest.kt`

- [ ] **Step 1: Update dispatcher implementation**

Change constructor to only take `ToolCallRegistry`. Return `ToolCallExecutionResult` directly for unknown tools and invalid arguments.

- [ ] **Step 2: Update call sites**

Replace `ToolCallDispatcher(registry, SpringboardToolCallExecutionErrorHandler)` and test error-handler setup with `ToolCallDispatcher(registry)`.

- [ ] **Step 3: Remove obsolete files and imports**

Delete the interface and Springboard implementation after all references are gone.

- [ ] **Step 4: Verify focused tests**

Run: `./gradlew composeApp:desktopTest --tests "com.strangeparticle.springboard.app.unit.ToolCallExecutorTest" --tests "com.strangeparticle.springboard.app.unit.ToolCallRegistryTest" --tests "com.strangeparticle.springboard.app.unit.tools.*"`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Verify full suite**

Run: `./gradlew composeApp:allTests`

Expected: `BUILD SUCCESSFUL`.
