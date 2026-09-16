## 1. Room schema version 3 (index)

- [ ] 1.1 Add `Index(value = ["outcome", "scheduled_at"])` to `DoseEntity` and bump the database version to 3.
- [ ] 1.2 Write `MIGRATION_2_3` (`CREATE INDEX ...`) and register it with the database builder.
- [ ] 1.3 Export the version 3 schema JSON to the app module's schemas directory and check it in.
- [ ] 1.4 Add a migration test to the existing harness: seed a version 2 database with a medication, a schedule and a dose row, migrate to version 3, and validate against the exported schema with all rows unchanged.
- [ ] 1.5 Add a DAO or query-plan test confirming `pending()`/`observePending()` use the new composite index.

## 2. Shared wake-cycle snapshot

- [ ] 2.1 Introduce a pending-dose snapshot type (pending doses plus a resolved lapse moment per dose) computed once per wake.
- [ ] 2.2 Change `DueDoses.invoke()` to accept the snapshot instead of calling `doseRepository.pending()` and `markMissedDoses.lapseAt(...)` itself.
- [ ] 2.3 Change `ComputeWakeSchedule.invoke()` to accept the snapshot instead of calling `doseRepository.pending()` and `markMissedDoses.lapseAt(...)` itself.
- [ ] 2.4 Update `ReminderCoordinator.wake()` to build the snapshot once (after `markMissedDoses()` runs) and pass it into `dueDoses(...)` and `computeWakeSchedule(...)`.
- [ ] 2.5 Update the unit tests for `DueDoses` and `ComputeWakeSchedule` to construct a snapshot directly instead of stubbing repository calls; keep every existing scenario passing unchanged.

## 3. Reuse the medication list across the wake

- [ ] 3.1 Change `RefreshPlannedDoses.invoke()` to return both the withdrawn dose ids and the medication list it already read.
- [ ] 3.2 Update `ComputeWakeSchedule.invoke()` to accept the medication list as a parameter instead of calling `medicationRepository.observeAll().first()` itself.
- [ ] 3.3 Update `ReminderCoordinator.wake()` to pass the medication list from `refreshPlannedDoses(...)`'s result into `computeWakeSchedule(...)`.
- [ ] 3.4 Update the affected unit tests for `RefreshPlannedDoses` and `ComputeWakeSchedule`.

## 4. Batch the per-row DB writes into transactions

- [ ] 4.1 Add a `@Transaction` composite method on `DoseDao` that applies a list of snapshot refreshes in one transaction; update `RoomDoseRepository.refreshSnapshots` to call it instead of looping.
- [ ] 4.2 Add a `DoseRepository.applyReminderOutcomes(updates: List<ReminderOutcomeUpdate>)` method backed by a `@Transaction` DAO method that records reminded state and clears snoozes for a batch of doses in one transaction.
- [ ] 4.3 Update `ReminderCoordinator.wake()`'s `due.forEach` loop to collect the per-dose updates and apply them in one call to `applyReminderOutcomes(...)` after the loop, instead of calling the repository per dose.
- [ ] 4.4 Wrap `RoomDoseRepository.withdrawPlanned`'s body in a single `@Transaction` DAO-backed call so the id-collection queries and `deleteByIds` are atomic.
- [ ] 4.5 Add or update DAO tests covering the batched methods with more than one row, confirming atomicity and confirming the resulting rows match the previous per-row behaviour exactly.

## 5. Reminder delivery log

- [ ] 5.1 Add an in-memory line count to `ReminderDeliveryLog`, initialised once by reading the file, and updated on every `append()` instead of re-reading the file.
- [ ] 5.2 Make `trimIfNeeded()` run its read-and-rewrite only when the in-memory count exceeds `MAX_ENTRIES + TRIM_SLACK`, and reset the count after trimming.
- [ ] 5.3 Add or update a test confirming the log file is not read on every `record()` call, and that trimming still occurs correctly once the threshold is crossed.

## 6. Verification

- [ ] 6.1 Run the full unit test suite for `domain/scheduling`, `data/RoomDoseRepository`, `data/reminders` and confirm every existing scenario in `reminder-scheduling` and `reminder-delivery-resilience` still passes unchanged.
- [ ] 6.2 Run lint.
- [ ] 6.3 Run the instrumented tests covering alarm scheduling and Room migrations, since this change touches scheduling and database code (per CLAUDE.md).
- [ ] 6.4 Manually verify a wake cycle end-to-end on a device or emulator (a dose becomes due, is reminded, is answered, and the next alarm is set) to confirm no observable behaviour changed.
