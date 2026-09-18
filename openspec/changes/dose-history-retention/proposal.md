## Why

**GitHub Issue:** #30 (https://github.com/hexmasternl/pillsner/issues/30)

Pillsner keeps every dose record forever. The Usage history screen only ever shows up to 3 months, so a year-old (or older) intake record serves the user no purpose while still sitting on the device indefinitely. Per GitHub issue #30, dose/intake history older than 1 year should be removed automatically, and the removal must not be triggered early by a device clock that has been set forward, whether by accident or deliberately.

## What Changes

- Add a retention rule: dose rows (taken, skipped or missed) whose scheduled moment is more than 1 year in the past, by local calendar date, are deleted. Pending doses are never affected — the app never keeps a pending dose anywhere near that old.
- Run the purge as one more idempotent step in the existing daily wake/housekeeping cycle (`ReminderCoordinator`), the same cycle that already extends the rolling dose window once a day. No new background mechanism (no `WorkManager`, no new alarm, no new permission) is introduced.
- Add a trusted-clock guard: a persisted "trusted now" high-water mark anchored to the boot clock (`SystemClock.elapsedRealtime()`), which only ever advances by real elapsed time. The purge computes its "1 year ago" cutoff from this trusted value instead of the raw system wall clock, so a wall clock moved forward can delay a purge but never trigger one early. No network access and no new Android permission are required.
- Medications and Schedules are never touched by this change; only dose/intake history rows are ever deleted.

## Capabilities

### New Capabilities
- `dose-history-retention`: defines what counts as expired dose history, when and how it is purged, and the trusted-clock guard that protects the purge from an unreliable wall clock.

### Modified Capabilities
(none — this change adds a new step to the existing wake cycle and reuses the existing `doses` schema without changing any existing capability's requirements)

## Impact

- **Domain**: a new use case that computes the retention cutoff (local calendar date, 1 year back) from a trusted-now value, and deletes qualifying dose rows through `DoseRepository`.
- **Data**: a new `DoseRepository` method for the bounded delete; no new Room schema version expected, since `doses.scheduled_at` already has an index (schema v5). The trusted-now high-water mark (a wall-clock/boot-clock pair) is persisted in the app's existing preferences store, not Room.
- **Wake cycle**: `ReminderCoordinator`'s wake body gains one more idempotent step, run every wake, alongside the existing refresh and missed-dose steps.
- **No new permissions, no network access.**
