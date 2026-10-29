## Context

Play Console flags "Edge-to-edge may not display for all users" (#72). The same warning was handled in #14 (`edge-to-edge-deprecated-theme-attributes`, archived 2026-09-22 and released to `main`). That change removed deprecated bar attributes from the phone's `Theme.Pillsner` and confirmed the phone `MainActivity` already calls `enableEdgeToEdge()`. The warning is still reported, so something #14 did not cover is causing it.

Current state:

- **Phone** (`src/app`): `MainActivity` (a `FragmentActivity`) calls `enableEdgeToEdge()` before `super.onCreate()`. `PillsnerTheme` sets bar icon contrast through `WindowCompat.getInsetsController`. Top-level destinations sit inside `NavigationSuiteScaffold` and pass `WindowInsets.safeDrawing.only(Top + Horizontal)` so the bottom inset is left to the navigation bar. Detail and full-screen destinations (dose detail, PIN setup, About, reminder diagnostics, medicine history, medication form, schedule editor, legal documents and acceptance) each use `Scaffold`, some with the default `contentWindowInsets` and some with a custom one. Forms add `imePadding()`. Nobody has checked these screen by screen on API 35+.
- **Watch** (`src/wear`): `MainActivity` is a plain `ComponentActivity` that calls `setContent` without `enableEdgeToEdge()`. `release.yml` builds `:wear:bundleRelease` and uploads it to Play next to the phone bundle, with the shared `targetSdk` of 37. The Wear Compose `AppScaffold` and `ScreenScaffold` already handle round-screen padding and the time text.

## Goals / Non-Goals

**Goals:**
- Every activity Pillsner ships enables edge-to-edge explicitly, so the Play recommendation clears for both bundles.
- Every phone screen keeps content and actions clear of system bars, cutouts and the IME, with each inset applied exactly once.
- A regression guard, so a future activity or screen can't quietly bring the problem back.

**Non-Goals:**
- No change to the phone `MainActivity`, `Theme.kt` or `themes.xml`, which #14 already settled.
- No visual redesign. Padding values stay the design-system `Spacing` tokens, and only inset handling changes.
- No change to the reminder notification or full-screen intent layout. That surface is a notification, not an activity window.

## Decisions

- **D1: Fix the watch with `enableEdgeToEdge()`, not a manifest opt-out.** `windowOptOutEdgeToEdgeEnforcement` is deprecated and ignored from API 36, so it isn't an option. Calling `enableEdgeToEdge()` before `super.onCreate()` matches the phone and is what the Play message asks for. On Wear it is effectively a no-op visually, because watches have no status or navigation bar in the phone sense and the Wear scaffolds already pad for the round screen and time text. That makes it low risk. *Alternative considered:* leave the watch alone and dismiss the Play warning. Rejected: the warning would return with every watch release, and behaviour would differ between Wear OS versions.
- **D2: Audit first, change only what is broken.** Each destination and dialog gets checked on an API 35+ emulator with gesture and 3-button navigation, in portrait and in landscape with a cutout, and with the keyboard open on forms. Fixes use `Scaffold(contentWindowInsets = …)`, `WindowInsets.safeDrawing` / `safeContent` with `.only(...)`, `consumeWindowInsets` to prevent doubling inside `NavigationSuiteScaffold`, and `imePadding()`. Hard-coded dp is never used. The audit results are recorded as a table in `tasks.md` so reviewers can see which screens were checked and what changed. *Alternative considered:* a blanket `safeDrawingPadding()` on the `NavHost`. Rejected: it would stop backgrounds and top app bars drawing behind the bars and would double-pad every screen that already handles insets.
- **D3: Guard test by source scan, plus targeted Compose tests.** A JVM unit test in each module scans that module's `src/main` for classes extending an `Activity` type and asserts each one calls `enableEdgeToEdge(` before `super.onCreate(`. It also scans manifests and theme XML for `windowOptOutEdgeToEdgeEnforcement`. This follows the house pattern of guard tests (compare the medication-delete guard) and needs no device. The dose detail and medication form screens get instrumented Compose semantics tests that assert the primary action's bounds lie inside the safe drawing area of the test window. *Alternative considered:* screenshot tests. Rejected for now, because no screenshot tooling is set up and adding it would mean a new dependency.
- **D4: Manual test cases are documented, not just performed.** Edge-to-edge depends on real system UI that tests only approximate. The manual matrix goes in the change's `tasks.md` and is ticked with the device or emulator and API level used: phone API 35/36/37 in gesture and 3-button modes, phone API 26–34, and a Wear OS 5+ emulator in round and square shapes.

## Risks / Trade-offs

- [The watch warning may be caused by something other than the missing call, such as a library using deprecated APIs] → Mitigation: after release, check Play Console's recommendation per bundle. If it persists, the report names the offending API, and that becomes a follow-up issue.
- [Adding `enableEdgeToEdge()` on Wear changes window flags on older Wear OS versions] → Mitigation: run the manual Wear matrix on a round and a square emulator. The spec requires the time text and every entry to stay visible.
- [A source-scan guard can be fooled, for example by a call inside a helper] → Mitigation: the guard accepts a direct call only. That is deliberate, because a single obvious line in `onCreate` is the convention this project wants, and anyone hitting the guard sees why in its failure message.
- [An audit fix on one screen shifts its layout slightly] → Mitigation: the fix runs through `pillsner-ui-review` and changes only inset modifiers. Design-system spacing tokens stay unchanged.

## Migration Plan

No data or schema involved. The change ships in the next release of both bundles. If Play still reports the warning after the release, reopen #72 with the Play report attached. Rollback means reverting the feature PR.

## Open Questions

- Does Play Console attribute the recommendation to the phone bundle, the watch bundle or both? The issue doesn't say. The fix covers both either way, but confirming it in Play Console after release will close the question.
