# AppleScript Resources

These scripts support macOS desktop integration: browser-window preparation before
URL activations, and opening terminals for `term` activations.

Supported browsers:
- Google Chrome
- Safari

Unsupported browsers fall back to normal URL opening without dedicated new-window integration.

Supported terminals:
- Terminal
- iTerm

Files:
- `chrome_new_window.applescript` - activates Google Chrome and opens a new window
- `safari_new_window.applescript` - activates Safari and opens a new window
- `terminal_new_window.applescript` - opens a new Terminal window running the command
- `terminal_front_window.applescript` - runs the command in Terminal's front window (new window if none)
- `iterm_new_window.applescript` - opens a new iTerm window running the command
- `iterm_front_window_new_tab.applescript` - opens a new tab in iTerm's front window (new window if none)

Debugging:
- Open any `.applescript` file in Script Editor to inspect and run it manually.
- Run from the shell with `osascript <path-to-script>`.
- The first run may trigger a macOS Automation permission prompt.

Development note:
- The desktop Kotlin integration is intentionally structured so AppleScript failures can either
  fall back quietly or be surfaced during local development.
  The current switch is `SURFACE_APPLESCRIPT_ERRORS` in `composeApp/src/desktopMain/kotlin/com/strangeparticle/springboard/app/platform/DesktopBrowserAutomation.kt`.
