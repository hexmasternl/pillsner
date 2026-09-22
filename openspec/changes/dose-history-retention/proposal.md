## Why

**GitHub Issue:** #30 (https://github.com/hexmasternl/pillsner/issues/30)

The app currently keeps every dose row forever. Nothing in the product surfaces usage data beyond
"3 months" (`medicine-usage-history`), so a year of dose/intake history is already more than the
app ever shows, and it is data with no purpose once it has aged past that. Keeping it around
indefinitely is unnecessary retention with no product benefit and a small but permanent cost:
every wake's pending-dose queries already scan a table that only grows
(`reminder-wake-cycle-db-efficiency`'s design explicitly treated unbounded dose-row growth as
accepted behaviour — this change revisits that decision now that the retention policy itself is in
scope). Bounding history to a rolling one year, purged automatically, keeps the app's data
footprint proportionate to what it actually uses, without adding a new background mechanism or any
permission the app does not already have.

This is a from-scratch re-implementation. An earlier attempt (PR #41, branch
`feature/dose-history-retention`) was built and closed without merging after review found a
critical flaw in its clock-tamper guard (a device reboot could be used to make a tampered wall
clock trusted again, defeating the guard's whole purpose) plus several smaller gaps. None of that
code exists in `development` today; this proposal starts over and designs around every problem
that closed the earlier attempt, not just the critical one.

## What Changes

- Add a purge that deletes `Dose` rows scheduled more than one calendar year in the past, run as
  one more idempotent step of the app's existing daily housekeeping wake — no `WorkManager`, no new
  periodic job, no new Android permission, no network access.
- Add a zero-permission, on-device guard against a tampered or reset system clock: a persisted
  "trusted now" high-water mark, anchored to the monotonic boot clock
  (`SystemClock.elapsedRealtime()`), that the purge uses instead of the raw wall clock. Trusted-now
  can only ever advance by as much real elapsed time as has actually passed, so a forward clock
  jump delays a purge but can never trigger one early — including across a reboot, which is exactly
  where the earlier attempt's guard broke.
- Widen the condition under which the daily housekeeping wake is armed, so a user with no currently
  active or scheduled medication (whose old dose history would otherwise never be visited by any
  wake) still gets it purged. **BREAKING** in the narrow sense that the housekeeping alarm now also
  arms based on stored dose history existing, not only on active scheduled medicines — a behaviour
  change to `reminder-scheduling`, not just an internal detail.
- No Room schema change: the purge is a single indexed `DELETE FROM doses WHERE scheduled_at < ?`,
  served by the existing single-column index on `scheduled_at` (schema v5).

## Capabilities

### New Capabilities
- `dose-history-retention`: the one-year purge of dose/intake history, the trusted-clock guard that
  computes its cutoff, and the rule that a purge failure never costs the user a reminder.

### Modified Capabilities
- `reminder-scheduling`: the "Daily refresh" requirement's housekeeping-alarm condition is widened
  so the alarm is armed whenever there is dose history that may need purging, not only when an
  active medication currently has a schedule.

## Impact

- `src/app/src/main/java/nl/hexmaster/pillsner/domain/scheduling/`: new `TrustedNow` and
  `PurgeExpiredDoseHistory` domain functions; `ComputeWakeSchedule`'s housekeeping condition
  changes.
- `src/app/src/main/java/nl/hexmaster/pillsner/data/reminders/`: new `TrustedClockStore`
  (DataStore-backed, modelled on the existing `ArmedAlarmStore`); `ReminderCoordinator.wake()` gains
  a purge step, last, in its own failure boundary that does not swallow cancellation.
- `src/app/src/main/java/nl/hexmaster/pillsner/domain/repository/DoseRepository.kt` and
  `RoomDoseRepository`/`InMemoryDoseRepository`/`DoseDao`: a new `deleteHistoryBefore(cutoff)`
  method.
- `src/app/src/main/java/nl/hexmaster/pillsner/di/AppContainer.kt`: wiring for the new
  dependencies.
- No new dependency, no new permission, no network access, no Room migration.
