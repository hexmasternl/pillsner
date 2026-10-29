# Tasks

## 1. Preconditions and test helper

- [ ] 1.1 Verify the `app-welcome-screen` scaffold exists and that both `MainActivity` classes still call `enableEdgeToEdge()` before `super.onCreate()` (both `EdgeToEdgeContractTest`s pass), and stop if either is missing
- [ ] 1.2 Add the inset test helper from design D1–D3 under `src/app/src/androidTest/java/nl/hexmaster/pillsner/ui/insets/`. It hosts content in an edge-to-edge `ComponentActivity`, applies named synthetic inset presets (gesture, 3-button, landscape with cutout, keyboard open) and provides `assertInsideSafeDrawingArea()`. Verify with a small self-test of the helper: a node padded with `safeDrawingPadding()` passes, an unpadded node at the bottom edge fails, and the insets read back in composition match the preset

## 2. Dose detail answers

- [ ] 2.1 Add a test that, with the 3-button preset, the dose detail screen's Taken, Snooze, Skip and Close buttons lie fully inside the safe drawing area and each can be clicked. Verify it passes on `development` as it is, or record the failure and continue to 2.3
- [ ] 2.2 Add the same assertion at `DeviceConfigurationOverride.FontScale(2f)`, and for the landscape-with-cutout preset with the answers scrolled into view. Verify both pass, or record the failure
- [ ] 2.3 Correct any failure from 2.1–2.2 with inset modifiers only (design D6), record it in the findings table below, and verify the tests now pass and `DoseDetailScreenTest` still passes

## 3. Medication form and schedule editor

- [ ] 3.1 Add a test that, with the keyboard-open preset and a field near the bottom focused, the medication form's Save action lies inside the safe drawing area and no gap larger than the bar's own padding token separates it from the keyboard (no doubled IME inset). Verify it passes, or record the failure
- [ ] 3.2 Add the same test for the schedule editor's Done action, and both actions at `FontScale(2f)` with the 3-button preset. Verify they pass, or record the failure
- [ ] 3.3 Correct any failure from 3.1–3.2 with inset modifiers only, record it in the findings table, and verify the tests now pass along with the existing medication form, schedule and `MedicineHistoryMenuTest` tests

## 4. Top-level screens above the bottom navigation

- [ ] 4.1 Add a test through the app shell (design D4) that seeds enough medicines to scroll, scrolls Medicines to the last tile with the 3-button preset, and asserts its bottom edge is at or above the bottom navigation bar with no band larger than the list's content padding token between them. Verify it passes, or record the failure
- [ ] 4.2 Repeat 4.1's assertion for Home (with enough doses to scroll) and for Settings (scrolled to Reset app). Verify they pass, or record the failure
- [ ] 4.3 Correct any failure from 4.1–4.2 with inset modifiers only (for example `consumeWindowInsets` where an inset is doubled), record it in the findings table, and verify the tests now pass along with `PillsnerAppNavigationTest`

## 5. Manual verification matrix

Record the emulator and API level for each row, using the same columns as the archived audit. Correct anything found the way 2.3 does, and add a test for it where design D2 can express it.

- [ ] 5.1 Phone API 37: dark theme, and the largest font in both navigation modes, for every destination and dialog in the archived audit table, including the rows marked "not checked" there (legal acceptance, unlock, discard, remove stock batch, verify identity, time picker, stock warning, the add-medicine form)
- [ ] 5.2 Phone API 35 and 36: gesture and 3-button, light and dark, largest font. Check Home, dose detail, Medicines, the medication form with the keyboard, and Settings. Bars are transparent, icon contrast is right and nothing is hidden
- [ ] 5.3 Phone API 26 and one of API 29–34: the same checks as 5.2, behaving the same as on API 35+
- [ ] 5.4 Wear OS round and square emulators at the largest watch font: the time text stays visible, every agenda entry and every details line is fully visible, and rotary and touch scrolling work

Findings:

| Screen / dialog | Where found (test or matrix row) | Gap | Fix |
| --- | --- | --- | --- |

## 6. Design review and verification

- [ ] 6.1 Run `pillsner-ui-review` on the files the archived change's inset fixes touched (medication form, schedule editor, legal document and legal acceptance screens) and on every file corrected in 2.3, 3.3, 4.3 or 5.x, and resolve each finding
- [ ] 6.2 From `src`, run `./gradlew :app:testDebugUnitTest :wear:testDebugUnitTest :app:lintDebug :wear:lintDebug` and report any failure verbatim
- [ ] 6.3 Run the affected instrumented tests through the `emulator-tests` skill, and confirm the `instrumented-tests` CI job passes on this change's pull request
