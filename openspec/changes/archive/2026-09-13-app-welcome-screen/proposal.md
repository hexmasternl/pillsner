## Why

Pillsner has no user interface yet: `src/` is empty and there is no screen a user can land on. The welcome screen is the first thing a user sees on every launch, so it needs to exist before medication, schedule or reminder screens can be reached, and it should immediately answer the one question the user opens the app for: *what do I need to take next?*

## What Changes

- Add the **welcome screen** as the app's start destination. It shows the Pillsner logo (a placeholder until the final artwork is designed) and the app title at the top.
- Below the header, the welcome screen lists the **upcoming doses**: for each dose the medication name, the dose (amount and form, e.g. "1 tablet") and the scheduled time. Doses are ordered soonest first and at most five are shown.
- Each upcoming dose is rendered as a styled **tile** (Material 3 card). When there are no upcoming doses the screen shows a friendly empty state instead of a blank area.
- Add a **bottom navigation bar** with three destinations: **Home** (the welcome screen), **Medicines** and **Settings**. The bar is visible on all three top-level destinations and highlights the current one. Home is selected on launch.
- Add the **app shell** required to host this: a single activity, a Compose navigation host with the three top-level routes, and the Pillsner Material 3 theme built from `docs/design-system.md` (brand colour schemes for light and dark, bundled Montserrat and Raleway, shapes, spacing tokens, intake status colours; dynamic colour off). The Medicines and Settings destinations are placeholder screens with a title only; their content is delivered by separate changes.
- **This change is the only change that creates the Gradle project.** It owns the project scaffold: Gradle wrapper, version catalog with pinned versions, the `app` module, application id, SDK levels, JDK toolchain, `.gitignore` and the manifest. Every other change starts from the precondition that this scaffold exists and stops if it does not.
- Introduce the minimal **domain contract** the welcome screen needs: an `UpcomingDose` model and a repository interface that exposes upcoming doses as a `Flow`. Until the medication and schedule changes land, the only implementation returns an empty list, so the welcome screen renders its empty state on a real device.

## Capabilities

### New Capabilities
- `welcome-screen`: The start screen of the app: header with logo and title, a list of at most five upcoming doses shown as tiles (medication, dose, scheduled time), an empty state, and live updates when the underlying doses change.
- `app-navigation`: The single-activity shell with a bottom navigation bar offering Home, Medicines and Settings, including which destination is shown on launch, how the selected destination is indicated, and how back navigation behaves between top-level destinations.

### Modified Capabilities

None. `openspec/specs/` contains no capabilities yet.

## Impact

- **Application code (`src/`)**: this change creates the Gradle project (Kotlin DSL, version catalog, wrapper), the single `MainActivity`, the `ui/theme` layer from the design system, the navigation host and the `home` feature area (welcome screen composables and view model), plus placeholder `medicines` and `settings` screens. Domain layer gains `UpcomingDose` and `UpcomingDosesRepository`; the data layer gains an empty in-memory implementation to be replaced by the Room-backed one later.
- **Scaffold ownership**: `app-login`, `app-medicine-overview`, `app-settings-language` and every later change depend on this scaffold and do not recreate any part of it. `app-login`'s Security section lives inside the Settings destination introduced here, and its lock gate wraps the navigation host introduced here. The build baseline (tool and library versions) is recorded in design decision D11 and pinned in the version catalog; later changes add their own libraries to that catalog at the versions their designs name.
- **Design system**: the welcome header, the dose tile, the empty state and the bottom navigation follow `docs/design-system.md` sections 8.1, 8.6, 8.7 and 8.10. The theme layer is built with the `pillsner-theme` skill and all UI passes `pillsner-ui-review` before the change is declared done.
- **Dependencies** (all first party, versions in design D11): Jetpack Compose BOM with Material 3 and the adaptive navigation suite, `androidx.navigation:navigation-compose`, `androidx.lifecycle` (view model and runtime Compose), `androidx.activity:activity-compose`, `androidx.core`, `kotlinx-coroutines`, `kotlinx-serialization` for type-safe routes, plus JUnit 4, AndroidX Test and Compose UI Test for tests. No third-party libraries, no network access, no telemetry.
- **Assets**: the placeholder capsule mark (design system section 7) as vector logo and adaptive launcher icon, swappable without code changes, and the five static font files with their licence text in `res/font/`.
- **Tests**: Compose semantics tests for the welcome screen (header, up-to-five tiles, empty state) and the navigation; unit tests for the view model's ordering and five-item cap.
- **README**: the "Features" section gains a line describing the welcome screen once the change is archived; the toolchain table in "Getting started" is kept in step with the version catalog.

## Non-goals

- Confirming, snoozing or skipping a dose from a tile. Tiles are informational in this change; intake actions arrive with the intake tracking change.
- Generating doses from schedules, or storing medications. The welcome screen consumes doses through a repository interface only.
- The Medicines and Settings screens' actual content.
- Final logo and branding artwork. The capsule placeholder from the design system is used until the artwork lands.
- Intake status states other than "Due" on the tile. The tile is built with the full status mapping from the design system, but only pending doses exist in this change.
