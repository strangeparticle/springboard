# UiBranding

UiBrand controls runtime UI styling only: `ColorScheme`, `Typography`, `Shapes`, `CustomColors`, `VectorImages`, and `DrawableResources`.

UiBrand provides resources for two things:

- Compose Multiplatform theme: `colorScheme`, `typography`, and `shapes` are applied through `MaterialTheme(...)` in `AppTheme`.
- Non-theme resources: `customColors`, `vectorImages`, and `drawableResources` are read directly from `LocalUiBrand.current` in composables.

This split exists because the theme supports broad theme roles, while Springboard also needs app-specific tokens and resources that do not belong in the CMP theme model.

## Setting the current brand

The active brand is declared directly in:

- `composeApp/src/commonMain/kotlin/com/strangeparticle/springboard/app/ui/brand/AppTheme.kt`

`AppTheme` currently provides `StrangeParticleBrand()`:

To switch brands, change `StrangeParticleBrand()` in `AppTheme.kt` to your brand factory.

## Why there are two color systems?

Springboard intentionally has two sets of color definitions:

- `colorScheme` for configuring the Compose Multiplatform Theme colors (`primary`, `surface`, `error`, etc.)
- `CustomColors` for app-specific tokens that are too narrow for Material roles (navbar, guidance panel, settings links, source indicators, etc.)

## Runtime access in composables

- Theme values: `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.shapes`
- Custom colors/vector-images/drawable-resources: `LocalUiBrand.current.customColors`, `LocalUiBrand.current.vectorImages`, and `LocalUiBrand.current.drawableResources`

`LocalUiBrand` is provided by `AppTheme` via `CompositionLocalProvider`.
