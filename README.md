# Springboard

Quickly access app/microservice resources.

Springboard is a resource launcher that organizes URLs (dashboards, logs, etc) and CLI commands
(open project in an IDE) into a three-dimensional grid: **Environment x App x Resource** that is
then navigable either purely via keyboard, or via a visual grid.

The project is currently focused on providing a desktop release. A web/WASM skeleton also exists
in-tree for experimentation, but it is not part of the initial supported release surface.

## Install

You can build the project yourself from source, or download a binary from the github project's
releases page.

## Quick Start

On the desktop app, open a springboard file with **Cmd+O** (or File -> Open).

An example springboard is available at `springboard-example.json`.

See [Springboard File Format](./README/README_Springboard_File_Format.md) for the full specification.

On macOS desktop builds, browser-window integration is enabled by default. This will open
a new browser window before activating urls (activated urls will open in that new browser 
window).

Springboard currently provides browswer-window integration support for Safari and Google Chrome
on OS X.

Other browsers and/or other OS's fall back to normal URL opening without the dedicated new-window
behavior. The first time Springboard opens a URL, macOS may prompt you to allow the app or terminal
that launched it to control your browser. The exact wording varies by macOS version.

## License

Licensed under the BSD 3-Clause License. See `LICENSE` and [Licensing Notes](./README/README-Licensing.md).

## Further Reading

- [Springboard File Format](./README/README_Springboard_File_Format.md) — config file specification and examples
- [Developers](./README/README-Developers.md) — build, run, and test instructions
- [Licensing Notes](./README/README-Licensing.md) — what gets bundled, where it lives, and what the app shows in the legal dialog
