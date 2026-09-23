## 1. Preconditions

- [ ] 1.1 Verify the `app-welcome-screen` scaffold exists (Gradle project, catalog, `app` and `wear` modules, both `MainActivity` classes, `PillsnerTheme`) and stop if it does not
- [ ] 1.2 Confirm both modules still target the catalog `targetSdk` (37) and that neither manifest nor theme sets `android:windowOptOutEdgeToEdgeEnforcement`

## 2. Watch activity

- [ ] 2.1 Call `enableEdgeToEdge()` in `src/wear/src/main/kotlin/nl/hexmaster/pillsner/wear/MainActivity.kt` before `super.onCreate()`, with a KDoc line that points at this change and issue #72
- [ ] 2.2 Check the wear module resolves `androidx.activity.enableEdgeToEdge` from its existing `activity-compose` dependency, with no new catalog entry

## 3. Guard tests

- [ ] 3.1 Add a JVM unit test in `src/app/src/test` that scans `src/app/src/main` for every class extending an `Activity` type and fails unless each one calls `enableEdgeToEdge()` before `super.onCreate()`. The failure message names the file and this requirement.
- [ ] 3.2 Add the same guard test for `src/wear/src/main` in `src/wear/src/test`
- [ ] 3.3 Extend both guards to fail when any manifest or `res/values*/themes.xml` in the module sets `android:windowOptOutEdgeToEdgeEnforcement`
- [ ] 3.4 Show each guard fails by temporarily removing the call, then restore it

## 4. Phone inset audit

- [ ] 4.1 On an API 35+ emulator with gesture navigation and again with 3-button navigation, check each destination and record the result in the table below: Home, Dose detail, Medicines, Medication form (add and edit), Schedule editor, Medicine history, Settings, About, Reminder diagnostics, Legal document, Legal acceptance, PIN setup, Unlock
- [ ] 4.2 Repeat 4.1 in landscape on an emulator profile with a display cutout
- [ ] 4.3 Check the keyboard on the medication form and schedule editor. The focused field and the save action must stay visible.
- [ ] 4.4 Check dialogs and pickers: stock warning, discard, add stock, remove stock batch, verify identity, date picker, time picker
- [ ] 4.5 Fix every gap found (missing, doubled or cutout-overlapping insets) using `Scaffold.contentWindowInsets`, `WindowInsets.safeDrawing`/`safeContent` with `.only(...)`, `consumeWindowInsets` or `imePadding()`. Never use fixed dp. Keep every fix to inset modifiers only.
- [ ] 4.6 Run `pillsner-ui-review` on every file changed in 4.5

Audit record (fill in during 4.1–4.4):

| Screen / dialog | Gesture | 3-button | Landscape + cutout | IME | Fix |
| --- | --- | --- | --- | --- | --- |
| _to be filled in_ | | | | | |

## 5. Compose tests

- [ ] 5.1 Add an instrumented Compose semantics test that shows the dose detail screen's answer buttons lie fully inside the safe drawing area and are clickable
- [ ] 5.2 Add an instrumented Compose semantics test that shows the medication form's save action stays inside the safe drawing area with the keyboard open
- [ ] 5.3 Add a test that the last Medicines list item can be scrolled fully above the bottom navigation bar

## 6. Manual verification matrix

- [ ] 6.1 Phone, API 35, 36 and 37: gesture and 3-button navigation, light and dark theme, largest font. Bars are transparent, icon contrast is right and nothing is obscured. Record the device or emulator used.
- [ ] 6.2 Phone, API 26 and one of API 29–34: the same checks as 6.1, with behaviour matching API 35+
- [ ] 6.3 Wear OS 5+ emulator, round and square: the time text is visible, every entry is fully visible at the largest font, and rotary and touch scrolling work

## 7. Verification and docs

- [ ] 7.1 From `src`, run `./gradlew :app:testDebugUnitTest :wear:testDebugUnitTest :app:lintDebug :wear:lintDebug` and report any failure verbatim
- [ ] 7.2 Run `./gradlew :app:connectedDebugAndroidTest` on an API 35+ emulator
- [ ] 7.3 Check whether the README needs to mention this. Expected: no change, because there are no new dependencies, permissions or toolchain bumps.
- [ ] 7.4 After the next release, check the Play Console recommendation for both bundles and note the outcome on issue #72
