# Springboard

Quickly access app/microservice resources.

Springboard is a resource launcher that organises URLs (dashboards, logs, etc) and CLI commands
(open project in an IDE) into a three-dimensional grid: **Environment x App x Resource** that is
then navigable either purely via keyboard, or via a visual grid.

## Install

## Quick Start

Springboard is configured by a single JSON file. On the desktop app, open one with **Cmd+O** (or File -> Open). See [Springboard File Format](./README/README_Springboard_File_Format.md) for the full specification.

On macOS desktop builds, browser-window integration is enabled by default. Springboard currently provides integration support for Safari and Google Chrome. Other browsers fall back to normal URL opening without the dedicated new-window behavior. The first time Springboard opens a URL, macOS may prompt you to allow the app or terminal that launched it to control your browser. The exact wording varies by macOS version.

## Further Reading

- [Springboard File Format](./README/README_Springboard_File_Format.md) — config file specification and examples
- [Developers](./README/README-Developers.md) — build, run, and test instructions

## License

Licensed under the BSD 3-Clause License. See `LICENSE`.
