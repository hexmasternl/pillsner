# Tasks

## 1. Preconditions and test helper

- [x] 1.1 Verify the `app-welcome-screen` scaffold exists and that both `MainActivity` classes still call `enableEdgeToEdge()` before `super.onCreate()` (both `EdgeToEdgeContractTest`s pass), and stop if either is missing
- [x] 1.2 Add the inset test helper from design D1–D3 under `src/app/src/androidTest/java/nl/hexmaster/pillsner/ui/insets/`. It hosts content in an edge-to-edge `ComponentActivity`, applies named synthetic inset presets (gesture, 3-button, landscape with cutout, keyboard open) and provides `assertInsideSafeDrawingArea()`. Verify with a small self-test of the helper: a node padded with `safeDrawingPadding()` passes, an unpadded node at the bottom edge fails, and the insets read back in composition match the preset

## 2. Dose detail answers

- [x] 2.1 Add a test that, with the 3-button preset, the dose detail screen's Taken, Snooze, Skip and Close buttons lie fully inside the safe drawing area and each can be clicked. Verify it passes on `development` as it is, or record the failure and continue to 2.3
- [x] 2.2 Add the same assertion at `DeviceConfigurationOverride.FontScale(2f)`, and for the landscape-with-cutout preset with the answers scrolled into view. Verify both pass, or record the failure
- [x] 2.3 Correct any failure from 2.1–2.2 with inset modifiers only (design D6), record it in the findings table below, and verify the tests now pass and `DoseDetailScreenTest` still passes

## 3. Medication form and schedule editor

- [x] 3.1 Add a test that, with the keyboard-open preset and a field near the bottom focused, the medication form's Save action lies inside the safe drawing area and no gap larger than the bar's own padding token separates it from the keyboard (no doubled IME inset). Verify it passes, or record the failure
- [x] 3.2 Add the same test for the schedule editor's Done action, and both actions at `FontScale(2f)` with the 3-button preset. Verify they pass, or record the failure
- [x] 3.3 Correct any failure from 3.1–3.2 with inset modifiers only, record it in the findings table, and verify the tests now pass along with the existing medication form, schedule and `MedicineHistoryMenuTest` tests

## 4. Top-level screens above the bottom navigation

- [x] 4.1 Add a test through the app shell (design D4) that seeds enough medicines to scroll, scrolls Medicines to the last tile with the 3-button preset, and asserts its bottom edge is at or above the bottom navigation bar with no band larger than the list's content padding token between them. Verify it passes, or record the failure
- [x] 4.2 Repeat 4.1's assertion for Home (with enough doses to scroll) and for Settings (scrolled to Reset app). Verify they pass, or record the failure
- [x] 4.3 Correct any failure from 4.1–4.2 with inset modifiers only (for example `consumeWindowInsets` where an inset is doubled), record it in the findings table, and verify the tests now pass along with `PillsnerAppNavigationTest`

## 5. Manual verification matrix

Record the emulator and API level for each row, using the same columns as the archived audit. Correct anything found the way 2.3 does, and add a test for it where design D2 can express it.

- [x] 5.1 Phone API 37: dark theme, and the largest font in both navigation modes, for every destination and dialog in the archived audit table, including the rows marked "not checked" there (legal acceptance, unlock, discard, remove stock batch, verify identity, time picker, stock warning, the add-medicine form) — partly done here (the four rows below, 3-button only); the rest handed to manual testing in the pull request
- [x] 5.2 Phone API 35 and 36: gesture and 3-button, light and dark, largest font. Check Home, dose detail, Medicines, the medication form with the keyboard, and Settings. Bars are transparent, icon contrast is right and nothing is hidden — not run here (no API 35/36 AVD); handed to manual testing in the pull request
- [x] 5.3 Phone API 26 and one of API 29–34: the same checks as 5.2, behaving the same as on API 35+ — not run here (no such AVD); handed to manual testing in the pull request
- [x] 5.4 Wear OS round and square emulators at the largest watch font: the time text stays visible, every agenda entry and every details line is fully visible, and rotary and touch scrolling work — not run here; handed to manual testing in the pull request

A tick in this section means the item is done or handed off under git-workflow lifecycle step 4, as each line says; it does not mean the whole matrix was run. The Findings table covers only what was actually run.

Matrix results (debug build, real system UI):

| Screen / dialog | Emulator, API | Mode | Result |
| --- | --- | --- | --- |
| Home (empty) | Pixel_10_Pro, 37 | dark, font 2.0, 3-button | Bars transparent, light status icons, nothing under the bars |
| Medicines (empty) | Pixel_10_Pro, 37 | dark, font 2.0, 3-button | Title and FAB clear of both bars |
| Legal acceptance | Pixel_10_Pro, 37 | dark, font 2.0, 3-button | Accept button and hint above the navigation bar |
| Settings, scrolled to the end | Pixel_10_Pro, 37 | dark, font 2.0, 3-button | Reset app fully above the bottom navigation |

Not run here, because the machine has only a Pixel_10_Pro API 37 phone AVD and watch AVDs, and the remaining rows need many dialogs reached by hand. These are handed to manual testing in the pull request (git-workflow step 4):

- 5.1, the rest: API 37 in gesture navigation, and in both modes the dose detail screen, unlock, discard, remove stock batch, verify identity, time picker, stock warning and the add-medicine form with the keyboard
- 5.2: phone API 35 and 36, gesture and 3-button, light and dark, largest font
- 5.3: phone API 26 and one of API 29–34, the same checks
- 5.4: Wear OS round and square at the largest watch font

The synthetic-inset tests in sections 1–4 cover the 3-button bar, the landscape cutout, the keyboard and font scale 2 for the dose answers, the form and schedule actions and the three top-level lists, on any API level CI runs.

Findings:

| Screen / dialog | Where found (test or matrix row) | Gap | Fix |
| --- | --- | --- | --- |
| (none) | Tests 2.1–4.2 and the four matrix rows run | No gap found; every new test passed on `development` as it was | No code change |

## 6. Design review and verification

- [x] 6.1 Run `pillsner-ui-review` on the files the archived change's inset fixes touched (medication form, schedule editor, legal document and legal acceptance screens) and on every file corrected in 2.3, 3.3, 4.3 or 5.x, and resolve each finding
- [x] 6.2 From `src`, run `./gradlew :app:testDebugUnitTest :wear:testDebugUnitTest :app:lintDebug :wear:lintDebug` and report any failure verbatim
- [ ] 6.3 Run the affected instrumented tests through the `emulator-tests` skill, and confirm the `instrumented-tests` CI job passes on this change's pull request

## Verification

- Unit tests: pass (`./gradlew test`, every module; also `:app:testDebugUnitTest :wear:testDebugUnitTest`)
- Lint: pass (`./gradlew lint`, and `:app:lintDebug :wear:lintDebug`)
- Assemble debug: pass
- UI review: compliant, no findings (`pillsner-ui-review` on the medication form, schedule editor, legal document and legal acceptance screens; this change corrected no UI file)
- Instrumented tests: selective, on Pixel_10_Pro API 37, emulator reused; the full suite runs in CI on the pull request
  - All 15 new tests pass: `EdgeToEdgeHostTest` 5, `DoseDetailInsetsTest` 3, `MedicationFormInsetsTest` 2, `ScheduleEditorInsetsTest` 2, `BottomNavigationInsetsTest` 3
  - Existing tests run alongside: `DoseDetailScreenTest` 9 of 9, `LegalGateNavigationTest` 9 of 9, the `ui.medicines.schedule` package, the `ui.medicines.form` package apart from one failure below, `PillsnerAppNavigationTest` 13 of 14, `DeadDestinationTest` 1 of 2
  - Sensitivity checks, each reverted: adding 48 dp under the Medicines list made `BottomNavigationInsetsTest` fail with "120.0 dp separates the last item from the bottom navigation bar, more than the list's 72.0.dp content padding: an inset applied twice?"; giving dose detail's top bar `WindowInsets.statusBars` only made the landscape test fail with "Node bounds Rect.fromLTRB(24.0, 108.0, 144.0, 228.0) reach outside the safe drawing area Rect.fromLTRB(96.0, 72.0, 2712.0, 1280.0)"
  - Failing locally, not caused by this change (it changes no main code): `DeadDestinationTest.theFormForAMedicineThatIsGone_closesBackToTheList`, `PillsnerAppNavigationTest.systemBackFromAbout_returnsToSettings` and `MedicationDetailsFlowTest.togglingTheSecondaryDetailsThenBackDoesNotAskToDiscard`, each with `java.lang.IllegalStateException: No compose hierarchies found in the app.` `DeadDestinationTest` fails the same way on `development` from the main checkout; the other two were not re-run there. Each was the first test of its run on a fresh install, which is when Home asks for the notification permission. Not fixed here: outside this change's scope.
- Critic review: `pillsner-app-critic` round 1 raised APP-1 (the landscape test could not fail), APP-2 (the large-font tests skipped titles and the status bar), APP-3 (the matrix ticks overstated what ran) and APP-4 (the helper's KDoc was wrong about keyboard animations). All four are addressed, along with its rotation question: auto-rotate is now restored to the device's own setting.
- Device scenarios: not run (no reminder, alarm or receiver code changed)
- Not run: Room checks (no schema change), wear contract (no `src/shared` change), website (no user-visible change)
- Manual testing still needed: the section 5 rows listed above as not run here
