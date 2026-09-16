## Why

Every reminder wake cycle — the hot path that runs on every alarm fire, snooze expiry, notification
action, boot, clock change and app start — currently does far more database and file I/O than the
work requires. A scan of `domain/scheduling`, `data/RoomDoseRepository.kt`, `data/reminders/` and
`data/db/DoseDao.kt` (recorded in `docs/todo.md`) found the same pending-dose query issued
redundantly by three use cases per wake, per-row loops that should be single batched statements or
transactions, and a reminder-delivery log that reads its entire file back on every write. None of
these are bugs — behaviour is correct today — but as the dose history grows unbounded (doses are
never deleted) the redundant work scales with it. Fixing the query pattern and the missing index
together avoids touching the same call sites twice.

## What Changes

- Compute the pending-dose list and each dose's lapse moment once per wake cycle in
  `ReminderCoordinator`/`ComputeWakeSchedule`, and pass the result into `MarkMissedDoses`,
  `DueDoses` and `ComputeWakeSchedule` instead of each calling `doseRepository.pending()` and
  `nextScheduledAtAfter(...)` independently.
- Have `ComputeWakeSchedule` reuse the medication list `RefreshPlannedDoses` already collected
  instead of independently collecting `medicationRepository.observeAll().first()` a second time.
- Wrap `RoomDoseRepository.refreshSnapshots` in a single `@Transaction` batch instead of one
  UPDATE/commit per dose.
- Replace the per-medication loop in `RoomDoseRepository.withdrawPlanned` with a single batched
  DAO query.
- Wrap the per-dose `recordReminded(...)`/`setSnooze(...)` calls in `ReminderCoordinator.wake()` in
  a single transaction, the same pattern as `refreshSnapshots`.
- Make `ReminderDeliveryLog.append()` avoid a full `file.readLines()` on every `record()` call by
  tracking the log's line count in memory (or only reading the file when its on-disk size suggests
  it is near the trim threshold).
- Add a Room schema version 3 with a composite index on `doses(outcome, scheduled_at)`, matching
  the `WHERE outcome IS NULL ORDER BY scheduled_at ASC` filter used by `observePending()` /
  `pending()`, with an accompanying migration and migration test.

None of this changes what the user sees or when a reminder fires, snoozes, or is recorded — every
scenario in `reminder-scheduling` and `reminder-delivery-resilience` continues to hold. This is an
internal efficiency change to the implementation of those requirements.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `medication-persistence`: the "Schema history" requirement gains a Version 3 entry (composite
  index on `doses(outcome, scheduled_at)`), and the "Migration test harness" requirement extends to
  cover a migration test from version 2 to version 3.

## Impact

- **Code**: `domain/scheduling/MarkMissedDoses.kt`, `domain/scheduling/DueDoses.kt`,
  `domain/scheduling/ComputeWakeSchedule.kt`, `domain/scheduling/RefreshPlannedDoses.kt`,
  `data/RoomDoseRepository.kt`, `data/reminders/ReminderCoordinator.kt`,
  `data/reminders/ReminderDeliveryLog.kt`, `data/db/DoseDao.kt`, `data/db/DoseEntity.kt`, plus a new
  Room migration and exported schema JSON for version 3.
- **Tests**: existing unit tests for the affected use cases and repository methods need updating to
  the new call shapes; a new migration test (version 2 → 3) is added to the existing harness;
  instrumented tests covering wake behaviour should be re-run since scheduling code changed, per
  CLAUDE.md.
- **No dependency, permission, or user-facing change.**
