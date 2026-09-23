## Why

**GitHub Issue:** #72 (https://github.com/hexmasternl/pillsner/issues/72)
**Pull Request:** #73 (https://github.com/hexmasternl/pillsner/pull/73)

Google Play Console again reports "Edge-to-edge may not display for all users" for Pillsner (#72), even though the earlier fix for the same warning (#14, archived as `edge-to-edge-deprecated-theme-attributes`) is already in production. That change only covered the phone app. Pillsner also ships a Wear OS bundle to Play, built from the same catalog with `targetSdk` 37, and its `MainActivity` never calls `enableEdgeToEdge()`. Play checks every bundle, so the watch app is the remaining cause. Separately, the phone's per-screen inset handling was declared out of scope in #14 and has never been checked screen by screen. On Android 15+ edge-to-edge is enforced and on Android 16+ it can't be opted out of, so a screen that misses an inset hides content, sometimes including the button that confirms a dose.

## What Changes

- Call `enableEdgeToEdge()` in the Wear OS `MainActivity` before `super.onCreate()`, so every activity Pillsner ships enables edge-to-edge explicitly and behaves the same on every supported API level.
- Audit every phone destination, dialog, picker and sheet for inset handling (status bar, navigation bar in gesture and 3-button mode, display cutout in landscape, IME on forms). Fix any missing or doubled padding with `WindowInsets`-based modifiers or `Scaffold.contentWindowInsets`, never hard-coded dp.
- Add a guard test that fails when an activity in either module stops enabling edge-to-edge. Add semantics-based Compose tests that the primary actions on the dose detail and medication form screens stay inside the safe drawing area.
- Add documented manual test cases for edge-to-edge on API 35+ and below, for the phone and the watch.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `app-theme`: the edge-to-edge requirement is widened. Every activity in every shipped module (phone and Wear OS) enables edge-to-edge at creation, and every phone screen and dialog keeps its content and actions clear of system bars, display cutouts and the keyboard.
- `wearable-app`: the Wear form factor requirement adds that the watch activity enables edge-to-edge explicitly and that its content stays clear of the screen edge and system UI.

## Impact

- `src/wear/src/main/kotlin/nl/hexmaster/pillsner/wear/MainActivity.kt`: one call added. The `activity-compose` dependency is already on the wear module's classpath through `setContent`.
- Phone screens under `src/app/src/main/java/nl/hexmaster/pillsner/ui/**` and `applock/ui/**`: only where the audit finds a real gap.
- New tests under `src/app/src/test`, `src/app/src/androidTest` and `src/wear/src/test`.
- No new dependencies, permissions, schema or API changes. No change to `MainActivity` (phone), `Theme.kt` or `themes.xml`, which are already correct.
