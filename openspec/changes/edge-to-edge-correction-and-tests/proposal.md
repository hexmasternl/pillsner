## Why

**GitHub Issue:** #113 (https://github.com/hexmasternl/pillsner/issues/113)

`edge-to-edge-insets` (#72, PR #73, archived 2026-10-29) made both activities enable edge-to-edge and fixed the phone screens its audit found broken, but it was archived with its proof unfinished. The `app-theme` requirement "Content stays clear of system bars, cutouts and the keyboard" now promises five things that no automated test checks: dose answers clear of the 3-button navigation bar, nothing under a landscape cutout, form actions above the keyboard, lists that scroll fully above the bottom navigation, and nothing hidden at the largest font. The manual audit only covered API 37 in light theme at the default font size. Dark theme, the largest font, API 26–36 and the watch were never checked, so gaps may still be shipping, and the button that confirms a dose is exactly the kind of content they hide.

## What Changes

- Add the instrumented Compose tests the archived change planned but never wrote: the dose detail answer buttons lie inside the safe drawing area and can be tapped, the medication form's Save action stays inside it with the keyboard open, and the last Medicines item scrolls fully above the bottom navigation bar.
- Add a test at the largest font scale that the dose detail answers and the medication form's Save action stay inside the safe drawing area, covering the spec's "Large font" scenario.
- Finish the manual verification matrix: phone API 35, 36 and 37 in gesture and 3-button navigation, light and dark theme, largest font; phone API 26 and one of API 29–34; Wear OS round and square at the largest font. Record each result.
- Correct every gap the tests and the matrix find, with inset modifiers only (`Scaffold.contentWindowInsets`, `WindowInsets.safeDrawing`/`safeContent` with `.only(...)`, `consumeWindowInsets`, `imePadding()`), never fixed dp, and with no change to layout or design-system spacing.
- Run `pillsner-ui-review` over the files the archived change's inset fixes touched, which it never did, and over any file this change corrects.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

(none) The behaviour is already specified by `app-theme` ("Every shipped activity enables edge-to-edge", "Content stays clear of system bars, cutouts and the keyboard") and `wearable-app` ("Wear form factor and accessibility"). This change makes the code meet those requirements and proves it, so it changes no requirement and sets `skip_specs: true`.

## Impact

- New instrumented tests under `src/app/src/androidTest/java/nl/hexmaster/pillsner/ui/` (dose, medicines and medicines/form), plus a small shared test helper for safe-area assertions. They run in the `instrumented-tests` CI job on every pull request from then on.
- Phone screens under `src/app/src/main/java/nl/hexmaster/pillsner/ui/**` and `applock/ui/**`, and the watch under `src/wear/src/main/**`: only where a test or the matrix finds a real gap, and only their inset modifiers.
- No new dependencies, permissions, schema, strings or user-facing features. The website needs no update: this corrects existing behaviour and adds nothing a user would read about.
