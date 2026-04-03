# AppleScript Resources

These scripts support macOS desktop browser-window preparation before URL activations.

Supported browsers:
- Google Chrome
- Safari

Unsupported browsers fall back to normal URL opening without dedicated new-window integration.

Files:
- `chrome/open_new_window.applescript` - activates Google Chrome and opens a new window
- `safari/open_new_window.applescript` - activates Safari and opens a new window

Debugging:
- Open any `.applescript` file in Script Editor to inspect and run it manually.
- Run from the shell with `osascript <path-to-script>`.
- The first run may trigger a macOS Automation permission prompt.

Development note:
- The desktop Kotlin integration is intentionally structured so AppleScript failures can either
  fall back quietly or be surfaced during local development.
  The current switch is `SURFACE_APPLESCRIPT_ERRORS` in `composeApp/src/desktopMain/kotlin/com/strangeparticle/springboard/app/platform/DesktopBrowserAutomation.kt`.
