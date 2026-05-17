# Springboard Mutators Throw Structured Errors Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `MutationResult` with mutators that return `Springboard` on success and throw a renamed structured error for validation failures.

**Architecture:** Move/rename `SpringboardToolCallError` into the mutator/domain layer as `SpringboardMutationError`. Mutator functions return `Springboard` directly and throw `SpringboardMutationError` for user/model-correctable domain failures. Tool handlers catch `SpringboardMutationError` at the typed handler boundary and convert it to `SpringboardToolCallHandlerResponse`.

**Tech Stack:** Kotlin Multiplatform, kotlinx.serialization, Gradle desktop/common tests.

---

### Task 1: Create Structured Domain Error and RED Test

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/strangeparticle/springboard/app/domain/mutator/SpringboardMutationError.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/strangeparticle/springboard/app/unit/tools/EntityCrudToolsTest.kt`

- [ ] **Step 1: Write failing test**

Add or update a direct mutator-focused assertion to expect `addApp(...)` to throw `SpringboardMutationError` for a duplicate id instead of returning `MutationResult.Failure`.

- [ ] **Step 2: Run focused tests and verify RED**

Run: `./gradlew composeApp:desktopTest --tests "com.strangeparticle.springboard.app.unit.tools.EntityCrudToolsTest"`

Expected: compile failure because `SpringboardMutationError` does not exist or assertion cannot compile against current `MutationResult` behavior.

- [ ] **Step 3: Add structured error class**

Create `SpringboardMutationError` with `errorMessage` and `code`, extending `RuntimeException(errorMessage)`.

### Task 2: Convert Mutators to Return Springboard

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/strangeparticle/springboard/app/domain/mutator/*.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/strangeparticle/springboard/app/domain/mutator/MutationResult.kt`

- [ ] **Step 1: Change mutator signatures**

Change every mutator returning `MutationResult` to return `Springboard`.

- [ ] **Step 2: Change failures to throws**

Replace `return MutationResult.Failure(code = ..., message = ...)` with `throw SpringboardMutationError(message, code)`.

- [ ] **Step 3: Change successes to raw Springboard**

Replace `return MutationResult.Success(updatedSpringboard)` with `return updatedSpringboard`.

- [ ] **Step 4: Delete MutationResult**

Delete `MutationResult.kt` after all references are removed from mutators.

### Task 3: Simplify Tool Handlers and Application Helper

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/strangeparticle/springboard/app/editio/SpringboardToolCallExecutionContext.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/strangeparticle/springboard/app/editio/toolcall/*ToolCallHandler.kt`

- [ ] **Step 1: Update applyMutation**

Change `applyMutation(tabId, result: MutationResult)` to `applyMutation(tabId, springboard: Springboard)` and make it success-only.

- [ ] **Step 2: Add typed-handler error conversion**

In each handler that calls a mutator, catch `SpringboardMutationError` around the typed handler body and return `context.errorResult(e.errorMessage, e.code)`.

- [ ] **Step 3: Simplify MoveActivatorToolCallHandler**

Remove `MutationResult` casts/checks. Let `addActivator(...)` and `removeActivator(...)` return `Springboard` or throw.

- [ ] **Step 4: Remove old tool-call error class**

Delete `SpringboardToolCallError` from `SpringboardToolCallExecutionContext.kt` after replacing missing-tab/empty-tab throws with `SpringboardMutationError` or another reused structured error name as approved.

### Task 4: Verify

**Files:**
- Test: `composeApp/src/commonTest/kotlin/com/strangeparticle/springboard/app/unit/tools/*.kt`
- Test: `composeApp/src/commonTest/kotlin/com/strangeparticle/springboard/app/unit/ToolCallRegistryTest.kt`

- [ ] **Step 1: Verify no MutationResult references remain**

Search for `MutationResult` and expect no matches in source/tests except deleted-file git status.

- [ ] **Step 2: Run focused tests**

Run: `./gradlew composeApp:desktopTest --tests "com.strangeparticle.springboard.app.unit.ToolCallRegistryTest" --tests "com.strangeparticle.springboard.app.unit.tools.*"`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run full suite**

Run: `./gradlew composeApp:allTests`

Expected: `BUILD SUCCESSFUL`.
