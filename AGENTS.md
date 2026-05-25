# Springboard Project Preferences

- For macOS browser integration, keep AppleScript as standalone `.applescript` files
- Prefer piping AppleScript source to `osascript` rather than extracting temp files unless a real file is truly required
- Detection logic, AppleScript execution, shell execution, and browser-window automation should live in separate files
- Interfaces and their implementations belong in separate, standalone files (one file for the interface, another for the impl). This applies to all interface/impl pairs, not just platform services.
- When an interface has multiple implementations, all names start with the interface name. The suffix alone describes what that particular impl does or is for — keep the distinguishing part at the end so names sort together and the role is immediately visible. Production impl: `*DefaultImpl`. Test double: `*InMemoryFake`. Example: `PlatformFileContentService`, `PlatformFileContentServiceDefaultImpl`, `PlatformFileContentServiceInMemoryFake`.
- If a feature should be developer-tunable, make it controllable by an explicit command-line arg rather than a hardcoded local constant
- Factor command-line arg parsing into a dedicated function or data structure with clearly named extracted values

## workflow preferences
- When a plan defines a bounded work unit (e.g., a per-group test cycle), complete the entire unit — including seams, wiring, and all automatable tests — before declaring it done. Do not stop at the "easy" portion and defer the rest.
- Keep commits narrowly scoped. Do not combine separable changes.
- Treat untracked docs/plans as important; protect them explicitly during git activity (include history rewrites).
- Platform/runtime facts should be owned by platform/runtime layers, not settings layers.
- Do not single out one platform integration as special unless there is a real technical difference.
- Acceptance tests are a readable coverage index first; keep test bodies thin and delegate to sibling scenario files.
- Keep platform-specific tests in platform-specific source sets.
- Use names that group well in filesystem/test runners.
- When renaming files, also rename matching top-level declarations (e.g. filenames and their containing class declarations should match).
- During refactors/renames, update all related references, imports, docs, and naming throughout the affected codepaths; do not stop at a partial/local fix.
- Plans should describe the current intended design only; do not include historical notes about prior plan versions or how decisions changed.
- When users correct plan content, incorporate the corrected end state directly; do not add meta notes about the correction process or conversation history.
- Store git worktrees under `~/Documents/development/` as siblings to the main repository checkout, not inside the repo directory.
- Name worktree directories as `<main-repo-name>_gwt_<branch-name>` (example: `springboard_gwt_guidance_indicator`).
- For this repository, do not ask for worktree location or naming convention; use the above defaults automatically. Branch naming itself remains unchanged.
- Do not commit specs/plans/design docs to this repository; store them in the sibling `springboard_resources` directory instead.
