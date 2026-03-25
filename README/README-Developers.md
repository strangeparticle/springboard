# Developers

Springboard is a [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) / [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform) project.

The public development story is desktop-first. The macOS desktop target is the primary supported path in this repo. The WASM target remains in-tree as an experimental skeleton for local tinkering, but it is not part of the initial supported release surface.

## Prerequisites

- **JDK 17+** — [Adoptium](https://adoptium.net/) or any distribution. The Gradle wrapper handles everything else; no separate Gradle install is needed.
- **Xcode Command Line Tools** (macOS) — required for native macOS packaging. Install with `xcode-select --install`.

NOTE: Java/JDK and Xcode are not necessary for running the binary, only for development/compilation.

## Clone

```shell
git clone <repo-url>
cd springboard
```

## Running the Desktop App (Most Common)

The desktop JVM target is the primary development mode. It has the full feature set: URL activation, command execution, file dialogs, and application menus.

It is also the easiest target to debug: run it from IntelliJ or via Gradle and use standard JVM breakpoints and hot-swap for small code changes.

### Run without a config (shows Open button after startup)

```shell
./gradlew :composeApp:run
```

### Run with a config pre-loaded

Pass a path to a springboard JSON file as a launch argument to skip the Open prompt and boot straight into the grid:

```shell
./gradlew :composeApp:run --args="springboard-example.json"
```

The sample config at `springboard-example.json` is intended as the public desktop example. The test fixture at `composeApp/src/commonTest/resources/springboard-test-fixture.json` is also suitable for local experimentation.

### Startup log lines

The app emits structured log lines at each startup milestone. If the app hangs or crashes, these indicate how far startup progressed:

| Log line | Milestone |
|---|---|
| `[Springboard] platform initialized` | JVM started |
| `[Springboard] launch config path: <path or none>` | Raw launch argument resolved |
| `[Springboard] config loading: <path>` | File read started |
| `[Springboard] config file not found: <path>` | Config file missing (app still opens) |
| `[Springboard] grid ready` | Grid rendered |
| `[Springboard] application ready` | Fully interactive |

A clean startup reaches `[Springboard] application ready`.

---

## Experimental WASM Target

The WASM target is currently experimental and unsupported for the initial public release. It remains in-tree for local tinkering only.

```shell
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

Notes:

- There is no supported public default config-loading story for WASM yet.
- Command activators are not supported on WASM.
- Browser popup restrictions apply to URL activation.

---

## Building

### Full build + all tests

```shell
./gradlew build
```

This compiles all targets, runs all tests, builds the desktop JAR, and produces the experimental WASM/browser distribution bundle. It does **not** create native macOS packages (`.dmg` / `.pkg`); use the packaging tasks below for those.

This target is the canonical "does everything work?" command.

### Tests only

```shell
./gradlew allTests
```

### Clean build

```shell
./gradlew clean build
```

Use this after major refactors or if you see stale-resource errors from the Compose resource generator.

---

## Tests

Tests live in `composeApp/src/commonTest/`. They cover domain model parsing, factory validation, index construction, and ViewModel state transitions. All tests are pure Kotlin with no platform dependencies and run on the JVM.

```shell
./gradlew allTests
```

A sample springboard config used by the tests is at:

```
composeApp/src/commonTest/resources/springboard-test-fixture.json
```

---

## Building Distributable Development Binaries (Local Use)

This creates an unsigned `.dmg` or `.pkg` for local use on your own machine. No signing keys, signing identities, provisioning profiles, or notarization setup are required in the public repo.

```shell
./gradlew :composeApp:packageDmg
```

Output: `composeApp/build/compose/binaries/main/dmg/Springboard-<version>.dmg`

```shell
./gradlew :composeApp:packagePkg
```

Output: `composeApp/build/compose/binaries/main/pkg/Springboard-<version>.pkg`

You will see a Gatekeeper warning when opening an unsigned build on macOS. Right-click → Open to bypass it on your own machine.

---

## Project Structure

```
composeApp/src/
├── commonMain/       # All domain logic, ViewModels, and Compose UI
├── commonTest/       # All unit tests
├── desktopMain/      # Desktop entry point and platform integrations; compiled for the desktop JVM target and packaged with an embedded Java runtime for distributable builds
└── wasmJsMain/       # Experimental browser entry point and JS interop
```

Platform-specific code is minimal — only OS integrations and entry points live outside `commonMain`.

---

## Dependency Management

Library versions are declared in `gradle/libs.versions.toml`. To update a dependency, change the version there and run `./gradlew build` to verify compatibility.

The Gradle wrapper version is set in `gradle/wrapper/gradle-wrapper.properties`. Update it with:

```shell
./gradlew wrapper --gradle-version=<version>
```
