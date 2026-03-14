# Developers

Springboard is a [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) / [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform) project targeting macOS/Linux desktop (JVM) and web (WASM).

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
./gradlew :composeApp:run --args="composeApp/src/wasmJsMain/resources/springboard.json"
```

The sample config at `composeApp/src/commonTest/resources/sample-springboard.json` is also suitable for this.

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

## Running the WASM App Locally

The WASM target is a browser-based version of Springboard. It auto-loads a config from a URL set in `index.html` — there is no file dialog.

### Start the dev server

```shell
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

Then visit [http://localhost:8080/](http://localhost:8080/).

### Local config

The dev server serves `wasmJsMain/resources/` as static files. The bundled `springboard.json` in that directory is loaded automatically. Edit it to point at your real resources, or swap in a different file.

The `index.html` in that directory sets the config URL:

```html
<script>window.springboardConfigUrl = "/springboard.json";</script>
```

Change the value to any accessible URL to load a different config without modifying the Kotlin code.

### Notes

- WASM requires serving over HTTP — you cannot open the built files directly from the filesystem because the app fetches `springboard.json` and browser security rules restrict `fetch()` and related asset loading from `file://` URLs.
- Command activators are not supported on WASM; those entries are silently ignored.
- Browser popup restrictions apply to URL activation (`window.open` is user-gesture restricted), so opening URLs works best when triggered directly by a click or keyboard action in the page.

---

## Building

### Full build + all tests

```shell
./gradlew build
```

This compiles all targets, runs all tests, builds the desktop JAR, and produces the WASM/browser distribution bundle. It does **not** create native macOS packages (`.dmg` / `.pkg`); use the packaging tasks below for those.

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
composeApp/src/commonTest/resources/sample-springboard.json
```

---

## Building Distributable Development Binaries (Local Use)

This creates an unsigned `.dmg` or `.pkg` for local use on your own machine. No signing keys required.

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
└── wasmJsMain/       # Browser entry point, JS interop, network config fetch
```

Platform-specific code is minimal — only OS integrations and entry points live outside `commonMain`.

---

## Dependency Management

Library versions are declared in `gradle/libs.versions.toml`. To update a dependency, change the version there and run `./gradlew build` to verify compatibility.

The Gradle wrapper version is set in `gradle/wrapper/gradle-wrapper.properties`. Update it with:

```shell
./gradlew wrapper --gradle-version=<version>
```

The application version (`appVersionName`) is declared in `gradle.properties`.
