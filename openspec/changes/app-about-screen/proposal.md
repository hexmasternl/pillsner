## Why

Pillsner tells the user nothing about itself. There is no way to see which version is installed, who made it, or what the name means — so a bug report cannot name a version, and the one piece of personality the product has (Pillsner = *Pills* + *Partner*) is only written down in the repository. An About screen is the conventional, expected home for all of it.

## What Changes

- Add an **About** section to the Settings screen, below Language and Security: one row that shows the installed version at a glance and opens the About screen.
- Add an **About** screen: a secondary destination with a top app bar, a back arrow and no bottom navigation.
- The About screen shows, in this order:
  - the product mark and the app name **Pillsner**;
  - where the name comes from — a *Pills* **Partner**;
  - the version name and version code;
  - the application id `nl.hexmaster.pillsner`;
  - the author, **Eduard Keilholz**.
- Introduce an `AppInfo` domain model and read the build's identity into it in the data layer, so the screen never reads build constants directly.
- All new text ships as English and Dutch string resources. Names (Pillsner, the author, the application id) are not translated.
- No new permissions, no network access, no links out of the app, no third-party dependency.

## Capabilities

### New Capabilities
- `app-about`: what the About screen shows, where its facts come from, how it is reached and how it behaves under large fonts and a screen reader.

### Modified Capabilities
- `app-navigation`: the Settings screen gains an About section as its last section, and the navigation host gains a non-top-level About destination that hides the bottom navigation bar and returns to Settings on back.

## Impact

- `src/app/build.gradle.kts`: enable the `buildConfig` build feature so the version and application id are available as generated constants.
- New `domain/model/AppInfo.kt` (no Android dependencies) and a data-layer provider that fills it.
- New `ui/settings/about/` package: the About screen and the Settings About section.
- Changed: `ui/settings/SettingsScreen.kt` (one more section), `ui/navigation/Routes.kt` (one more route), `ui/PillsnerApp.kt` (one more destination), `di/AppContainer.kt` (expose `AppInfo`).
- New strings in `values/strings.xml` and `values-nl/strings.xml`.
- Tests: unit test for the provider, Compose semantics tests for the section row and the screen, a navigation test for open and back.
