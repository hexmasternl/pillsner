## 1. Preconditions

- [x] 1.1 Verify the project scaffold exists under `src` (Gradle project, `app` module, manifest, `MainActivity`, `AppContainer`, theme) and stop if it does not; this change creates none of it
- [x] 1.2 Confirm `reminder-delivery-reliability` has been archived and `openspec/specs/reminder-delivery-resilience/spec.md` exists, so this change's delta has something to modify. Stop and say so if it has not
- [x] 1.3 Read `openspec/specs/reminder-delivery-resilience/spec.md` and this change's `design.md` before touching code

## 2. Stop asking for the exemption

- [x] 2.1 Remove the request branch from `BatteryOptimisationEffect` in `ui/home/BatteryOptimisationRequest.kt`, along with `requestBatteryExemption` and its `@SuppressLint("BatteryLife")`; keep `isIgnoringBatteryOptimisations`, `backgroundRunIntent`, the vendor intent table and `resolves`
- [x] 2.2 Rename the file and the composable to say what is left — it observes and routes, it no longer requests — and update the KDoc to cite this change's D1 rather than `reminder-delivery-reliability` D6
- [x] 2.3 Remove `shouldRequestBatteryExemption` and `onBatteryExemptionRequested` from `ui/home/HomeViewModel.kt`
- [x] 2.4 Remove `hasRequestedBatteryExemption`, `markBatteryExemptionRequested` and the `battery_exemption_requested` key from `data/reminders/ReminderPreferences.kt`, and correct its class KDoc, which currently describes two dialogs
- [x] 2.5 Update the call site in `ui/PillsnerApp.kt` to the reduced effect
- [x] 2.6 Remove the `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission and its comment block from `src/app/src/main/AndroidManifest.xml`; leave `<queries>`, `USE_EXACT_ALARM` and everything else untouched
- [x] 2.7 Delete or rewrite any test that asserted the dialog is shown once, including the `shouldRequestBatteryExemption` view-model tests

## 3. `plannedAt` on the dose

- [x] 3.1 Add `plannedAt: Instant` to `data/db/DoseEntity.kt` and to the `Dose` domain model in `domain/model/Dose.kt`, with KDoc saying it is the moment the row was first stored and is never rewritten
- [x] 3.2 Set `plannedAt` on insert in `RefreshPlannedDoses`/the dose repository, from the same `Clock` the rest of the scheduling code uses, and confirm the refresh's update path never rewrites it
- [x] 3.3 Bump `PillsnerDatabase.VERSION` and add `MIGRATION_3_4` in `data/db/Migrations.kt`: add the column `NOT NULL DEFAULT 0`, then backfill it from `scheduledAt` so every pre-existing row fails the "existed before it was due" test
- [x] 3.4 Add `MIGRATION_3_4` to `Migrations.ALL`
- [x] 3.5 Write the migration test for 3 → 4 alongside the existing ones: a row present before the migration has `plannedAt` equal to its `scheduledAt` afterwards
- [x] 3.6 Add DAO/repository test coverage that a dose inserted now carries a `plannedAt` earlier than its `scheduledAt`, and that a refresh of an existing pending dose leaves `plannedAt` alone

## 4. Record a silently missed reminder

- [x] 4.1 Add a nullable instant to `ReminderPreferences` for the moment of the most recent silent miss, with a `Flow` to read it, a suspend function to record it and one to clear it
- [x] 4.2 In `ReminderCoordinator.wake`, where `markMissedDoses()` returns the lapsed doses, record a silent miss for any lapsed dose with `firstRemindedAt == null` and `plannedAt` before `scheduledAt`, guarded on notifications being allowed
- [x] 4.3 Decide and implement how the coordinator learns that notifications are allowed — `ReminderNotifier` is the component that already knows; do not reach into Android framework types from the domain layer
- [x] 4.4 Clear the record in `domain/reset/EraseAllData.kt` (or `AppDataEraser`, wherever the reminder preferences are already cleared) so the danger-zone reset takes it with everything else
- [x] 4.5 Unit-test the rule directly: reminded-then-lapsed records nothing; lapsed-un-reminded with `plannedAt` before `scheduledAt` records; generated-already-lapsed records nothing; notifications denied records nothing
- [x] 4.6 Confirm nothing added here logs a medicine name or amount, and that any dose identifier stays at debug level

## 5. Home banner

- [x] 5.1 Replace `batteryExempt` in `ui/home/HomeUiState.kt` with the missed-reminder input, keeping the three-way precedence: notifications not allowed, then a reminder was missed, then alarms not exact
- [x] 5.2 Rename `ReminderProblem.BATTERY_OPTIMISED` to say what it now means, and update `WelcomeScreen.kt`'s mapping
- [x] 5.3 Feed the new input into `HomeViewModel` from `ReminderPreferences`, and clear it when the banner's action is activated
- [x] 5.4 Point the banner's action at `backgroundRunIntent()` unconditionally — the vendor screen when one resolves, the system battery-optimisation list otherwise — with no dependence on the exemption state
- [x] 5.5 Reword the banner string in `res/values/strings.xml` and every translated `values-*` copy: it reports that a reminder did not arrive, and names no cause. Take the copy from the `pillsner-designer` agent
- [x] 5.6 Resolve the design's open question: what the banner does on a device that is already exempt, has missed a reminder and has no vendor screen. Record the answer in `design.md` under D3 rather than leaving it in Open Questions
- [x] 5.7 Run the `pillsner-ui-review` skill over every file touched in this section and clear what it raises

## 6. Documentation

- [x] 6.1 Remove `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` from any permission list in `README.md` and `PRIVACY.md`, and check neither still describes the app as asking for a battery exemption
- [x] 6.2 Remove the battery-whitelist precondition from `openspec/changes/reminder-delivery-reliability/manual-tests.md`'s archived copy only if it has not yet archived; otherwise record in this change's own manual test notes that the precondition is obsolete
- [x] 6.3 Note in this change's manual tests: on a physical device, confirm no system dialog appears on first use with a scheduled medicine, and that the banner appears only after a dose lapses un-reminded

## 7. Verification

- [x] 7.1 Run the unit test task from `src` and report any failure verbatim
- [x] 7.2 Run the lint task from `src` and clear anything it raises, confirming the `BatteryLife` suppression is gone rather than merely silenced
- [ ] 7.3 Run the instrumented tests — alarm scheduling, notification posting, database migration, boot and unlock — granting only `POST_NOTIFICATIONS`, and confirm the battery whitelist precondition is genuinely no longer needed
- [ ] 7.4 Re-run the reminder tests inherited from `reminder-delivery-after-reboot` and `reminder-delivery-reliability` and confirm none regressed
- [x] 7.5 Inspect the merged manifest in `app/build/intermediates` and confirm `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is absent while `USE_EXACT_ALARM` remains

> **7.3 and 7.4 are outstanding.** No device or emulator was reachable in the session that applied
> this change, so the instrumented suite has not been run. Everything it covers compiles
> (`compileDebugAndroidTestKotlin` passes) and the migration test for 3 → 4 is written and waiting.
> The battery whitelist precondition those runs used to need is gone; see `manual-tests.md`.
