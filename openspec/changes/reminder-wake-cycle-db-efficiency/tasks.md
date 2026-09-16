## 1. Room schema version 5 (index)

Note: the database is already at version 4 in this repository (versions 1-3 cover unrelated earlier
changes), not version 2 as the original design assumed — this change adds version 5, not version 3.

- [x] 1.1 Add `Index(value = ["outcome", "scheduled_at"])` to `DoseEntity` and bump the database version to 5.
- [x] 1.2 Write `MIGRATION_4_5` (`CREATE INDEX ...`) and register it with the database builder.
- [x] 1.3 Export the version 5 schema JSON to the app module's schemas directory and check it in.
- [x] 1.4 Add a migration test to the existing harness: seed a version 4 database with a medication, a schedule and a dose row, migrate to version 5, and validate against the exported schema with all rows unchanged. (Written; compiles; not yet run — no device/emulator in this environment, see task 6.3.)
- [x] 1.5 Add a DAO or query-plan test confirming `pending()`/`observePending()` use the new composite index. (Written as an `EXPLAIN QUERY PLAN` assertion; compiles; not yet run, see task 6.3.)

## 2. Shared wake-cycle snapshot

- [x] 2.1 Introduce a pending-dose snapshot type (pending doses plus a resolved lapse moment per dose) computed once per wake. (`PendingSnapshot` + `buildPendingSnapshot`, built after `refreshPlannedDoses()` per the corrected design, not right after `markMissedDoses()`.)
- [x] 2.2 Change `DueDoses.invoke()` to accept the snapshot instead of calling `doseRepository.pending()` and `markMissedDoses.lapseAt(...)` itself.
- [x] 2.3 Change `ComputeWakeSchedule.invoke()` to accept the snapshot instead of calling `doseRepository.pending()` and `markMissedDoses.lapseAt(...)` itself.
- [x] 2.4 Update `ReminderCoordinator.wake()` to build the snapshot once (after `refreshPlannedDoses()` runs, per the corrected design) and pass it into `dueDoses(...)`; update the snapshot in memory to mirror the due-dose posting loop's writes, and pass the result into `computeWakeSchedule(...)` via `reconcileAlarms(WakeResult)`.
- [x] 2.5 Update the unit tests for `DueDoses` and `ComputeWakeSchedule` to construct a snapshot directly instead of stubbing repository calls; keep every existing scenario passing unchanged. (Verified: `./gradlew :app:testDebugUnitTest` — `DueDosesTest` 7/7, `ComputeWakeScheduleTest` 11/11, all passing.)

## 3. Reuse the medication list across the wake

- [x] 3.1 Change `RefreshPlannedDoses.invoke()` to return both the withdrawn dose ids and the medication list it already read. (`RefreshResult`.)
- [x] 3.2 Update `ComputeWakeSchedule.invoke()` to accept the medication list as a parameter instead of calling `medicationRepository.observeAll().first()` itself.
- [x] 3.3 Update `ReminderCoordinator.wake()` to pass the medication list from `refreshPlannedDoses(...)`'s result into `computeWakeSchedule(...)`.
- [x] 3.4 Update the affected unit tests for `RefreshPlannedDoses` and `ComputeWakeSchedule`. (Verified: `RefreshPlannedDosesTest` 19/19 passing.)

## 4. Batch the per-row DB writes into transactions

- [x] 4.1 Add a `@Transaction` composite method on `DoseDao` that applies a list of snapshot refreshes in one transaction; update `RoomDoseRepository.refreshSnapshots` to call it instead of looping.
- [x] 4.2 Add a `DoseRepository.applyReminderOutcomes(updates: List<ReminderOutcomeUpdate>)` method backed by a `@Transaction` DAO method that records reminded state and clears snoozes for a batch of doses in one transaction.
- [x] 4.3 Update `ReminderCoordinator.wake()`'s `due.forEach` loop to collect the per-dose updates and apply them in one call to `applyReminderOutcomes(...)` after the loop, instead of calling the repository per dose.
- [x] 4.4 Wrap `RoomDoseRepository.withdrawPlanned`'s body in a single `@Transaction` DAO-backed call so the id-collection queries and `deleteByIds` are atomic. (Moved into `DoseDao.withdrawPlanned`.)
- [x] 4.5 Add or update DAO tests covering the batched methods with more than one row, confirming atomicity and confirming the resulting rows match the previous per-row behaviour exactly. `refreshSnapshots`/`withdrawPlanned` were already covered with multiple rows at the repository level (same call shape, now transactional underneath); added two new tests for the new `applyReminderOutcomes` method. Written; compiles; not yet run — no device/emulator (task 6.3).

## 5. Reminder delivery log

- [x] 5.1 Add an in-memory line count to `ReminderDeliveryLog`, initialised once by reading the file, and updated on every `append()` instead of re-reading the file.
- [x] 5.2 Make `trimIfNeeded()` run its read-and-rewrite only when the in-memory count exceeds `MAX_ENTRIES + TRIM_SLACK`, and reset the count after trimming.
- [x] 5.3 Add or update a test confirming the log file is not read on every `record()` call, and that trimming still occurs correctly once the threshold is crossed. (Verified: `ReminderDeliveryLogTest` 6/6 passing, including two new tests: growing the file behind the log's back proves it isn't re-checking the real file size, and a repeat of the bounded-log test confirms trimming still happens off the in-memory count.)

## 6. Verification

- [x] 6.1 Run the full unit test suite for `domain/scheduling`, `data/RoomDoseRepository`, `data/reminders` and confirm every existing scenario in `reminder-scheduling` and `reminder-delivery-resilience` still passes unchanged. (`./gradlew :app:testDebugUnitTest --rerun`: 420/420 passing, 0 failures, 0 errors, across the whole app module — not just the touched packages.)
- [x] 6.2 Run lint. (`./gradlew :app:lintDebug`: 0 errors, 52 warnings, all pre-existing `MissingQuantity`/`PluralsCandidate` translation warnings unrelated to this change.)
- [x] 6.3 Run the instrumented tests covering alarm scheduling and Room migrations, since this change touches scheduling and database code (per CLAUDE.md). Correction: the Android SDK and an AVD (`Pixel_9`) were in fact available (`adb` just wasn't on `PATH`); booted the emulator and ran `./gradlew :app:connectedDebugAndroidTest` for the full instrumented suite (47 classes). Every test this change touches passes: `PillsnerDatabaseMigrationTest` 5/5 (including the new `migrate4To5_addsTheOutcomeIndexAndKeepsEverything`), `DoseDaoTest` 25/25, `ReminderRecoveryTest` 17/17, `ReminderWatchdogTest` 4/4, `ReminderAlarmReconcileTest` 6/6. Two pre-existing failures elsewhere in the suite are unrelated to this change: `ReminderWakeTest.aWakeThatCannotReadAnything_leavesTheArmedAlarmsExactlyWhereTheyAre` fails identically on `main` (verified byte-for-byte via a throwaway `git worktree` checkout of `main` — same assertion, same values, `armRetryOrGiveUp()` is untouched by this change), and `LanguageSectionTest` has 2 locale-string failures in the unrelated `ui/settings/language` capability.
- [ ] 6.4 Manually verify a wake cycle end-to-end on a device or emulator (a dose becomes due, is reminded, is answered, and the next alarm is set) to confirm no observable behaviour changed. Not yet done — the automated instrumented suite (`ReminderWakeTest`, `ReminderRecoveryTest`, `ReminderWatchdogTest`, `ReminderAlarmReconcileTest`) already exercises this exact scenario end to end, but a real manual walkthrough on the running `Pixel_9` emulator has not been performed.
