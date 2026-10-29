## Context

See proposal.md for why. The relevant current state:

- Both `MainActivity` classes call `enableEdgeToEdge()` before `super.onCreate()`, guarded by `EdgeToEdgeContractTest` in each module's `src/test`. That half of the `app-theme` spec is already proven.
- The inset fixes from `edge-to-edge-insets` are on `development`: the medication form and schedule editor `bottomBar` use `windowInsetsPadding(safeDrawing.only(Horizontal + Bottom))` and dropped their own `imePadding()`, and the legal screens' `contentWindowInsets` gained `Bottom`. Top-level destinations sit in `NavigationSuiteScaffold` with `safeDrawing.only(Top + Horizontal)`. The audit table is in `openspec/changes/archive/2026-10-29-edge-to-edge-insets/tasks.md` and lists what was never checked.
- Existing screen tests (`DoseDetailScreenTest`, `MedicinesScreenTest`, the form tests) use `createComposeRule()`. Its host `ComponentActivity` never enables edge-to-edge, so the window fits system windows, Compose sees zero insets, and an inset bug can't show up in those tests.
- Instrumented tests run in CI on the `phoneApi37` managed device (Pixel 6, API 37, `google-atd`) and the `watchApi37` device (Wear OS Small Round). These are fixed devices: one navigation mode, no cutout, and possibly no on-screen keyboard.

## Goals / Non-Goals

**Goals:**
- Each `app-theme` "Content stays clear…" scenario that can be checked with a test has one that runs on the CI managed device and gives the same result on any emulator.
- The scenarios a test can't reach credibly are covered by a recorded manual matrix, as the original change intended.
- Any gap found is corrected in the same change.

**Non-Goals:**
- No screenshot testing and no new test dependency (the archived change's D3 still holds).
- No new managed devices or CI matrix (3-button, cutout or other API levels in CI). That would multiply CI time for a layout concern that synthetic insets already cover.
- No layout, spacing or copy changes beyond inset modifiers.

## Decisions

- **D1: Host the screen in an edge-to-edge activity.** The new tests use `createAndroidComposeRule<ComponentActivity>()` and call `enableEdgeToEdge()` on the activity before `setContent`, so the window behaves like the app's `MainActivity`. *Alternative considered:* launching the real `MainActivity` and navigating to each screen. Rejected: it pulls in the legal gate, the app lock and seeded data, and makes each test slow and indirect. One end-to-end case through the app shell is kept for the bottom-navigation scenario only (D4).
- **D2: Use synthetic window insets.** Each test builds a `WindowInsetsCompat` with known values and dispatches it to the host's decor view with `ViewCompat.dispatchApplyWindowInsets`: a 48 dp navigation bar (3-button mode), a 24 dp status bar, a display cutout on one side in landscape, and an IME of about 300 dp for the keyboard case. It then asserts that the target node's bounds lie inside the window minus those insets. Compose reads insets from the same dispatch, so this exercises the real inset code path, independent of the device's navigation mode, cutout or keyboard. *Alternative considered:* driving the real system UI (switching navigation mode over adb, showing the soft keyboard). Rejected: the ATD image may not ship a keyboard, navigation-mode overlays need a reboot-like settle, and both are flaky in CI. Real system UI stays in the manual matrix (D5).
- **D3: Use a small shared helper rather than per-test geometry.** A helper in `src/app/src/androidTest/.../ui/insets/` applies a named inset preset and offers `assertInsideSafeDrawingArea()` on a `SemanticsNodeInteraction`. It compares `getBoundsInRoot` with the root size minus the dispatched insets, with a 0.5 dp tolerance for rounding. This keeps each test to a few readable lines and puts the rounding rule in one place.
- **D4: Test bottom-navigation scrolling through the app shell.** The "last item scrolls fully above the bottom navigation" scenario depends on how `NavigationSuiteScaffold` and the Medicines list share insets, which a screen in isolation doesn't reproduce. That test hosts the top-level shell (the same entry `PillsnerAppNavigationTest` uses) with a seeded list long enough to scroll, scrolls to the last tile, and asserts two things: its bottom edge is at or above the bottom navigation bar's top edge, and the gap between them is no larger than the list's content padding token. That catches both a hidden item and a doubled inset band.
- **D5: Test the largest font with `DeviceConfigurationOverride.FontScale(2f)`.** 2.0 is the largest system font scale on current Android. The override comes with the existing `ui-test` artifact, so no dependency is added. It applies to the dose detail answers and the medication form's Save action with the 3-button preset from D2.
- **D6: Keep the manual matrix and fix order the same.** The tests are written first, against `development` as it is. A red test or a matrix finding gets a fix limited to inset modifiers (the archived change's D2 list), then the test goes green. Each fix is recorded in a findings table in `tasks.md` like the original audit's, so reviewers can see what was found where.

## Risks / Trade-offs

- [Synthetic insets can disagree with what a real device dispatches, for example cutout shapes or gesture insets] → Mitigation: the manual matrix (6.x tasks) runs on real emulator system UI for each scenario, and its results are recorded next to the test results.
- [The real system may dispatch insets again after the synthetic ones and overwrite them] → Mitigation: the helper dispatches after the first composition, waits for idle, and asserts on `WindowInsets` read back from composition before it checks bounds. If they don't match the preset, the test fails with a clear message rather than passing silently.
- [A 0.5 dp tolerance could hide a real off-by-one] → Mitigation: every gap that matters here is at least the 24 dp gesture bar, so half a dp can't mask a real problem.
- [The matrix finds a gap that can't be fixed with inset modifiers alone] → Mitigation: stop and raise it. That would be a layout change, and it belongs in its own proposal.

## Migration Plan

No data, schema or release-process impact. The tests land with any corrections in one feature PR into `development`, and the `instrumented-tests` job runs them on every later PR. Rollback means reverting that PR.
