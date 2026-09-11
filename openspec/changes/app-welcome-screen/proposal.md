## Why

Pillsner has no user interface yet: `src/` is empty and there is no screen a user can land on. The welcome screen is the first thing a user sees on every launch, so it needs to exist before medication, schedule or reminder screens can be reached, and it should immediately answer the one question the user opens the app for: *what do I need to take next?*

## What Changes

- Add the **welcome screen** as the app's start destination. It shows the Pillsner logo (a placeholder until the final artwork is designed) and the app title at the top.
- Below the header, the welcome screen lists the **upcoming doses**: for each dose the medication name, the dose (amount and form, e.g. "1 tablet") and the scheduled time. Doses are ordered soonest first and at most five are shown.
- Each upcoming dose is rendered as a styled **tile** (Material 3 card). When there are no upcoming doses the screen shows a friendly empty state instead of a blank area.
- Add a **bottom navigation bar** with three destinations: **Home** (the welcome screen), **Medicines** and **Settings**. The bar is visible on all three top-level destinations and highlights the current one. Home is selected on launch.
- Add the **app shell** required to host this: a single activity, a Compose navigation host with the three top-level routes, and a Material 3 theme. The Medicines and Settings destinations are placeholder screens with a title only; their content is delivered by separate changes.
- Introduce the minimal **domain contract** the welcome screen needs: an `UpcomingDose` model and a repository interface that exposes upcoming doses as a `Flow`. Until the medication and schedule changes land, the only implementation returns an empty list, so the welcome screen renders its empty state on a real device.

## Capabilities

### New Capabilities
- `welcome-screen`: The start screen of the app: header with logo and title, a list of at most five upcoming doses shown as tiles (medication, dose, scheduled time), an empty state, and live updates when the underlying doses change.
- `app-navigation`: The single-activity shell with a bottom navigation bar offering Home, Medicines and Settings, including which destination is shown on launch, how the selected destination is indicated, and how back navigation behaves between top-level destinations.

### Modified Capabilities

None. `openspec/specs/` contains no capabilities yet.

## Impact

- **Application code (`src/`)**: this change creates the Gradle project (Kotlin DSL, version catalog), the single `MainActivity`, the Material 3 theme, the navigation host and the `home` feature area (welcome screen composables and view model), plus placeholder `medicines` and `settings` screens. Domain layer gains `UpcomingDose` and `UpcomingDosesRepository`; the data layer gains an empty in-memory implementation to be replaced by the Room-backed one later.
- **Coordination with `app-login`**: that in-progress change also states it will create the project scaffold if absent. Whichever change is applied first creates the scaffold; the other builds on it. `app-login`'s Security section will live inside the Settings destination introduced here, and its lock gate wraps the navigation host introduced here.
- **Dependencies** (all first party): Jetpack Compose BOM, Material 3, `androidx.navigation:navigation-compose`, `androidx.lifecycle:lifecycle-viewmodel-compose`, `androidx.activity:activity-compose`, `kotlinx-coroutines`. No third-party libraries, no network access, no telemetry.
- **Assets**: a placeholder vector logo and adaptive launcher icon that can be swapped for the designed artwork without code changes.
- **Tests**: Compose semantics tests for the welcome screen (header, up-to-five tiles, empty state) and the bottom navigation; unit tests for the view model's ordering and five-item cap.
- **README**: the "Features" section gains a line describing the welcome screen once the change is archived; the technology table is unaffected.

## Non-goals

- Confirming, snoozing or skipping a dose from a tile. Tiles are informational in this change; intake actions arrive with the intake tracking change.
- Generating doses from schedules, or storing medications. The welcome screen consumes doses through a repository interface only.
- The Medicines and Settings screens' actual content.
- Final logo and branding artwork.
