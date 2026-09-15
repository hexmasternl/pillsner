## 1. Verify the ground

- [ ] 1.1 Confirm the project scaffold exists and builds: `src/` holds the Gradle project, `app/` module, `AppContainer`, `PillsnerTheme`. Stop and report if any part is missing — this change does not create scaffold.
- [ ] 1.2 Confirm the Settings screen still composes its sections as `item`s of one `LazyColumn` in `ui/settings/SettingsScreen.kt`, and that the About section is currently last.
- [ ] 1.3 Confirm `PillsnerDatabase` declares `medications`, `schedules` and `doses`, and note its current `VERSION` so task 4.2 can assert it is unchanged.

## 2. Domain: the erase operation

- [ ] 2.1 Add `domain/reset/AppDataEraser.kt`: a single-method interface `suspend fun eraseAll()`, with KDoc saying it empties every stored medicine, schedule and dose in one transaction and touches no setting. No Android imports.
- [ ] 2.2 Add `domain/reset/ReminderTeardown.kt` (`fun cancelAll()`) and `domain/reset/ReminderRefresh.kt` (`fun refresh()`) — two one-method ports so the use case can settle notifications and the alarm without an Android dependency.
- [ ] 2.3 Add `domain/reset/EraseAllData.kt`: an operator-invoke use case that runs, in this fixed order, `eraser.eraseAll()`, `teardown.cancelAll()`, `refresh.refresh()`. KDoc the order and why the irreversible step goes first (design D3).
- [ ] 2.4 Unit-test `EraseAllData`: the three collaborators are called exactly once each, in that order (fakes recording a sequence). Assert the erase runs before either side effect.

## 3. Data: clearing the database

- [ ] 3.1 Add `data/reset/RoomAppDataEraser.kt` implementing `AppDataEraser` as `withContext(Dispatchers.IO) { database.clearAllTables() }`. KDoc why `clearAllTables()` and not per-DAO deletes (design D2), and that no DAO gains a delete.
- [ ] 3.2 Add `ReminderNotifier.cancelAll()` calling `notificationManager.cancelAll()`. KDoc that it is correct only because reminders are the only notification Pillsner posts, and that a later second kind of notification must narrow it to the reminder group.
- [ ] 3.3 Wire it up in `di/AppContainer.kt`: build `RoomAppDataEraser(database)`, bind `ReminderTeardown` to `reminderNotifier::cancelAll` and `ReminderRefresh` to `{ reminderCoordinator.requestWake(WakeReason.MEDICATIONS_CHANGED) }`, and construct `EraseAllData`.

## 4. Data tests

- [ ] 4.1 Room test: seed medicines, schedules and doses covering taken, skipped and missed outcomes, run the eraser, assert all three tables are empty.
- [ ] 4.2 Room test: after the erase the schema version equals the value noted in 1.3 and no migration ran.
- [ ] 4.3 Room test: a medicine inserted immediately after the erase, on the same database instance, is stored and readable — the database is still open and usable.
- [ ] 4.4 Room test: a collector on `medicationRepository.observeAll()` and one on the pending-doses flow each receive an empty list after the erase, without re-subscribing.
- [ ] 4.5 Confirm the existing `medication-persistence` guard test — no medication delete on the DAO — still passes unchanged. Do not modify it.

## 5. View model

- [ ] 5.1 Add `ui/settings/reset/ResetUiState.kt`: `dialogVisible`, `confirmationAccepted`, `inProgress`; and `ResetEffect` with the single member `Erased`.
- [ ] 5.2 Add `ui/settings/reset/ResetViewModel.kt` with `onResetTapped`, `onDismiss`, `onConfirmationToggled`, `onConfirmed`, and a `Channel`-backed effect flow. `onDismiss` clears `confirmationAccepted`. `onConfirmed` returns early unless `confirmationAccepted` and not `inProgress`.
- [ ] 5.3 Register `ResetViewModel` in the `viewModelFactory` in `AppContainer`.
- [ ] 5.4 Unit-test the view model: confirm is a no-op while unticked; ticking then confirming erases once; a second confirm while in progress erases nothing more; dismissing clears the tick so reopening starts unticked; `Erased` is emitted exactly once per erase.

## 6. UI — run `pillsner-theme` first, then build through `pillsner-ui-build`

- [ ] 6.1 Add the nine strings to `values/strings.xml` and `values-nl/strings.xml`: section heading, section explanation, section button, dialog title, erased-body paragraph, kept-body paragraph, checkbox label ("I understand all data will be erased permanently"), confirm button, snackbar message.
- [ ] 6.2 Add `ui/settings/reset/DangerZoneSection.kt` per design D6: `HorizontalDivider`, `headlineSmall` heading in `colorScheme.error` marked `heading()`, `bodyMedium` explanation in `onSurfaceVariant`, and a full-width error-container `Button` at `Sizes.primaryActionHeight`. Every colour, size and spacing value from a theme token. Ship `@PreviewLightDark` and a `fontScale = 2f` preview.
- [ ] 6.3 Add `ui/settings/reset/ResetAppDialog.kt` per design D7: `AlertDialog` with `shapes.large`, `headlineMedium` title, two body paragraphs, a `toggleable(role = Role.Checkbox)` row at `Sizes.minTouchTarget` minimum height, an error-container confirm button gated on `accepted`, and a `TextButton` cancel. `onDismissRequest` maps to cancel. Ship `@PreviewLightDark` previews for both the unticked and ticked states, plus a `fontScale = 2f` preview.
- [ ] 6.4 Add stable test tags for the section (heading, button) and the dialog (dialog, checkbox, confirm, cancel), in the `*TestTags` object style the other settings sections use.
- [ ] 6.5 Wire the section into `SettingsScreen` as the last `item` after About, hoisting the state and callbacks as parameters the way the other sections do. Collect `ResetEffect` in the existing `LaunchedEffect` and show the snackbar on `Erased`.
- [ ] 6.6 Update the `SettingsScreenPreview` with the new parameters so the preview still compiles and shows the danger zone.

## 7. UI tests

- [ ] 7.1 Semantics test: the danger zone heading is reachable as a heading, the Reset app button has the button role, and both are present below About.
- [ ] 7.2 Semantics test: tapping Reset app shows the dialog and the confirm button is disabled; ticking the checkbox enables it; unticking disables it again.
- [ ] 7.3 Semantics test: tapping the checkbox *label* toggles the checkbox, and the row is announced with the checkbox role, its label and its ticked state.
- [ ] 7.4 Semantics test: cancel, system back and a tap outside each close the dialog and erase nothing; reopening shows the checkbox unticked and confirm disabled.
- [ ] 7.5 Semantics test: rotation while the dialog is open and the box is ticked keeps the dialog open, the box ticked and confirm enabled.
- [ ] 7.6 Semantics test: after a confirmed reset the Settings screen is still shown and the confirmation snackbar appears.
- [ ] 7.7 Run the `pillsner-ui-review` skill over `ui/settings/reset/` and the `SettingsScreen` diff; fix every violation before moving on.

## 8. End-to-end and platform behaviour

- [ ] 8.1 Instrumented test: with a reminder notification posted, run the reset and assert no notification remains posted.
- [ ] 8.2 Instrumented test: with an alarm scheduled, run the reset and assert no alarm is pending afterwards.
- [ ] 8.3 Instrumented test: language, app-lock enabled state, stored PIN verifier, biometric flag and legal acceptance are all identical before and after a reset, and the same PIN still unlocks.
- [ ] 8.4 Instrumented test: after a reset, Home shows its empty state and Medicines shows its empty state without re-navigating.
- [ ] 8.5 Instrumented test: returning by back navigation to a destination opened for a medicine the reset removed closes that destination and returns to the medicines list without crashing. Fix any id-keyed destination that does not already do this.
- [ ] 8.6 Verify by inspection that no notification action, deep link, shortcut, intent filter or restored navigation state can reach the reset; record the check in the test source as a comment or an assertion over the navigation graph.

## 9. Verify and close out

- [ ] 9.1 Run the unit test task from `src/`. Report any failure verbatim.
- [ ] 9.2 Run lint from `src/`, including the missing-translation check. Report any failure verbatim.
- [ ] 9.3 Run the instrumented test task from `src/` — this change touches notification, alarm and database code. Report any failure verbatim.
- [ ] 9.4 Update `README.md` if its feature list names settings features, so the reset is disclosed there too.
- [ ] 9.5 Re-read `openspec/changes/app-settings-reset/specs/` and confirm every requirement has a corresponding test or a documented manual check; note anything covered only manually.
