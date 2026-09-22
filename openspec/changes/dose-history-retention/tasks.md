## 1. Trusted clock

- [x] 1.1 Add `TrustedNow` (`domain/scheduling/`), a pure function/class that, given the persisted
  `(trustedNowMillis, anchorElapsedRealtimeMillis)` sample (or none), the current wall clock and
  the current boot-clock (`elapsedRealtime`) reading, returns the updated sample to persist plus
  whether trusted-now is validated for use this observation — implementing design.md D2 exactly:
  no-prior-sample seeds and reports unvalidated; boot-clock-went-backward freezes the mark and
  reseeds only the anchor; the ordinary case advances the mark by
  `max(0, min(wallDelta, elapsedDelta))`. No Android framework dependency.
- [x] 1.2 Add `TrustedClockStore` (`data/reminders/`), a DataStore-backed store for the two longs.
  **Correction found during implementation** (recorded in design.md D2): modelled on the plain
  `ReminderPreferences` DataStore, not `ArmedAlarmStore`'s device-protected one — the purge only
  ever runs inside `ReminderCoordinator.wake()`, which is unreachable before the first unlock after
  a reboot, so device-protected storage was never needed here. The Android-touching reads
  (`System.currentTimeMillis()`, `SystemClock.elapsedRealtime()`) live in a separate
  `TrustedClockGuard` that composes the store with `TrustedNow`.
- [x] 1.3 Unit test `TrustedNow` covering: ordinary elapsed time; forward clock jump capped at the
  real elapsed delta; backward clock move does not regress the mark; boot clock going backward
  (reboot) freezes the mark and does not reseed from the wall clock; no prior sample seeds and
  reports unvalidated; a later observation after a seed advances normally and reports validated.
  (`TrustedNowTest`, 7 cases, all passing.)

## 2. Purge domain logic

- [x] 2.1 Add `PurgeExpiredDoseHistory` (`domain/scheduling/`), a pure function taking a validated
  trusted-now instant and a `Clock` (for `Clock.zone`, read fresh every call — never cached), that
  returns the one-year-ago local-calendar-date cutoff as an `Instant`. Uses `Instant.atZone(zone)`
  rather than `LocalDate.ofInstant` — the latter needs API 34, above this app's minSdk 26; lint
  caught this (`NewApi`) and it was fixed before verification.
- [x] 2.2 Unit test `PurgeExpiredDoseHistory` covering: an ordinary one-year cutoff; the leap-day
  boundary (29 February); a cutoff that spans a daylight-saving transition; the current system zone
  being read at call time, not cached (two calls with different injected zones on the same instant
  produce different cutoffs). (`PurgeExpiredDoseHistoryTest`, 4 cases, all passing.)

## 3. Repository and DAO

- [x] 3.1 Add `DoseDao.deleteHistoryBefore(cutoff: Instant): Int` —
  `DELETE FROM doses WHERE scheduled_at < :cutoff` — and `DoseDao.hasAnyDose(): Boolean` —
  `SELECT EXISTS(SELECT 1 FROM doses LIMIT 1)`. No entity, index or schema version change.
- [x] 3.2 Add `DoseRepository.deleteHistoryBefore(cutoff: Instant): Int` and
  `DoseRepository.hasAnyDose(): Boolean` to the domain interface; implement both in
  `RoomDoseRepository` (delegating to the DAO) and `InMemoryDoseRepository` (delegating to its
  in-memory store).
- [x] 3.3 Updated `DoseDao`'s "production never removes a dose" comment and
  `DoseRepository.observeHistoryFor`'s "history is never withdrawn" KDoc to describe this purge as
  the one narrow, documented exception, so neither comment goes stale.
- [x] 3.4 DAO test: seed doses older and newer than a cutoff, call `deleteHistoryBefore`, confirm
  only the older ones are removed and the return value matches the count deleted; confirmed the
  medication row itself is never touched; added an `EXPLAIN QUERY PLAN` test proving the delete is
  served by `index_doses_scheduled_at`, mirroring the existing `pendingQuery_usesTheOutcomeScheduledAtIndex`
  test's approach (`DoseDaoTest`, 4 new cases). Instrumented — see 6.3 for execution status.
- [x] 3.5 DAO test for `hasAnyDose()`: false on an empty table, true once any dose exists, false
  again after `deleteAll()` (`DoseDaoTest.hasAnyDose_reflectsWhetherAnythingHasEverBeenStored`).
  Instrumented — see 6.3.

## 4. Housekeeping alarm gate

- [x] 4.1 Added a `hasDoseHistory: Boolean` parameter to `ComputeWakeSchedule.invoke(...)`; widened
  the daily-refresh condition to `if (anyMedicineProducesDoses(medications) || hasDoseHistory)`.
- [x] 4.2 Updated every existing `ComputeWakeScheduleTest` call site to pass an explicit
  `hasDoseHistory` argument (matching the scenario's intent) rather than relying on a default.
- [x] 4.3 Added `ComputeWakeScheduleTest` cases for: no active medication and no dose history (no
  daily-refresh moment armed); no active medication but `hasDoseHistory = true` (daily-refresh
  moment is armed); an active scheduled medication (daily-refresh armed regardless of history).
  All 17 `ComputeWakeScheduleTest` cases pass.

## 5. Wiring the purge into the wake cycle

- [x] 5.1 In `ReminderCoordinator.wake()`, after the due-dose posting loop and before building the
  final `WakeResult`, added `runDoseHistoryPurge()` exactly as design.md D3 specifies: observes
  trusted-now via `trustedClockGuard`, and only if validated, computes the cutoff and calls
  `deleteHistoryBefore`; the try/catch re-throws `CancellationException` before catching ordinary
  `Exception`, so a wake timeout is never swallowed.
- [x] 5.2 Passed `doseRepository.hasAnyDose()` into `computeWakeSchedule(...)` in
  `reconcileAlarms()` alongside the existing snapshot and medication list.
- [x] 5.3 Wired `TrustedClockGuard(TrustedClockStore(applicationContext))` and
  `PurgeExpiredDoseHistory(clock)` into `AppContainer`, passed into `ReminderCoordinator`; both
  constructor parameters default to a no-op (`trustedClockGuard = null`,
  `purgeExpiredDoseHistory = PurgeExpiredDoseHistory(clock)`) so every existing test/preview call
  site that does not care about the purge is unaffected.
- [x] 5.4 `ReminderCoordinator` test coverage for the purge step, in a new `ReminderHistoryPurgeTest`:
  a first wake with no persisted trusted-clock sample seeds the clock and skips the purge but still
  completes normally; a later wake with a validated trusted-now purges the expired dose and keeps
  the recent one; a purge failure (a `DoseRepository` decorator whose `deleteHistoryBefore` always
  throws) does not block alarm reconciliation; a wake that times out while a slow purge is running
  (a decorator that delays past a short `timeoutMillis`) is retried rather than reported complete.
  Instrumented — see 6.3.

## 6. Verification

- [x] 6.1 Ran the unit test task (`:app:testDebugUnitTest`): **all 438 tests pass**, including the
  new and changed ones from this change (10 `TrustedNowTest`, 4 `PurgeExpiredDoseHistoryTest`, 3 new
  and 13 updated `ComputeWakeScheduleTest` cases).
- [x] 6.2 Ran the lint task (`:app:lintDebug`): **clean** — 0 errors, only the same 53 pre-existing
  deprecation warnings this repository already carries (Compose adaptive-info/menu-anchor API
  renames), none of them new or related to this change.
- [x] 6.3 An Android SDK was located on this machine (`C:\Program Files (x86)\Android\android-sdk`)
  but lacked the compileSdk 37.0 platform and could not be written to; a working copy with
  `platform-tools`, `platforms;android-37.0` and `build-tools;37.0.0` was installed into this
  session's scratchpad directory instead, which is what made 6.1/6.2 (and `:app:assembleDebug`,
  also run and green) possible at all. No emulator or physical device was available, so the
  instrumented test task itself (`connectedDebugAndroidTest`) was **not executed**. Every
  instrumented test this change adds or touches was confirmed to compile cleanly via
  `:app:compileDebugAndroidTestKotlin` (also fully green, zero new warnings). Saying this plainly
  rather than rounding it up to "done": the new `DoseDaoTest`, `TrustedClockStoreTest` and
  `ReminderHistoryPurgeTest` cases are correct by construction and by review, not by having been
  run on a device.
- [x] 6.4 Added `DoseDaoTest` cases (3.4, 3.5) and a new `TrustedClockStoreTest` (persistence,
  read-after-write, outliving the object that stored it, and clearing) to the instrumented suite —
  see 6.3 for why they were compile-checked rather than run.
- [x] 6.5 Manual test case for the clock-tamper scenarios that cannot be automated without a real
  device, to be run once real hardware or an emulator is available:
  1. On a fresh install (or after clearing app data), let the app run at least one ordinary wake
     so `TrustedClockStore` has a first sample (Settings → Developer options → confirm no crash;
     the Reminder Diagnostics screen's log will show a `WAKE` entry with no `HISTORY_PURGED` yet).
  2. Advance the device's date by more than a year in system settings, then trigger a wake (open
     the app, or wait for the next alarm). Confirm no dose younger than a year of *real* elapsed
     time was purged (check via the medicine usage history screen, or by inspecting the `doses`
     table with `adb shell` + `sqlite3` on a debug build) — the tampered date must not have moved
     trusted-now forward by more than the real time that passed since step 1.
  3. Reboot the device (physically or via `adb reboot`) while the date is still tampered forward,
     then unlock and trigger a wake. Confirm the guard still does not purge based on the tampered
     time — trusted-now should be exactly where it was after step 2, not reseeded to the new
     (still-tampered) wall clock.
  4. Set the device's date back to the correct time (or simply wait for real time to catch up past
     the tampered value), trigger another wake, and confirm the purge resumes normally: dose rows
     genuinely more than a year old by real elapsed time are removed, and the Reminder Diagnostics
     log shows a `HISTORY_PURGED` entry.
- [x] 6.6 Verification results are recorded inline above (6.1-6.5). Summary for a reviewer: unit
  tests, lint and the debug assembly all ran green in this environment against a real (locally
  installed) Android SDK; every instrumented test compiles; nothing was executed on an emulator or
  device, so 6.3's compile-only status and 6.5's manual test case are what still need a real device
  before this change can be considered fully verified end to end.

### Fix found in PR #50 review

Copilot's review of PR #50 found a real, high-severity gap this change's first draft had missed:
`TrustedNow`'s first observation (no persisted sample) seeded `trustedNowMillis` straight from the
raw wall clock. That observation itself correctly skipped the purge, but the reasoning stopped
there — the *next*, ordinary observation treated that seed as an already-trusted baseline and
advanced it by only a small real elapsed delta, handing it back fully validated. A wall clock
tampered forward before the very first observation would therefore still drive a purge, just one
wake later than immediately, exactly the kind of "eventually validated from a wrong starting point"
gap the original design missed.

Fixed by adding a `knownGoodFloorMillis` parameter to `TrustedNow.observe`, sourced from a new
`DoseRepository.latestKnownMoment()` (`MAX(planned_at)` across every stored dose — a wall-clock
reading the app itself already took, so it cannot postdate a tamper that happens later). A first
observation now clamps its seed to `min(currentWallMillis, knownGoodFloorMillis)` and validates
immediately when a floor exists; only a genuinely history-free first run (nothing yet a wrong seed
could delete) still seeds-and-skips as before. Full reasoning in `design.md` D2's "Correction found
during PR review", including the one residual gap this cannot close (a clock tampered before the
very first dose is ever recorded, and left tampered forever after) recorded honestly under Risks
rather than left implicit.

Also fixed, per the same review pass: `PurgeExpiredDoseHistoryTest`'s "reads the zone fresh, never
caches" test used two separate `PurgeExpiredDoseHistory` instances, each queried once — which would
pass even if the class cached its zone in a field, since neither instance was ever asked twice. It
now uses one instance queried twice, with the underlying clock's zone mutated in between.

New/changed: `TrustedNow.kt`, `TrustedClockGuard.kt`, `ReminderCoordinator.kt` (pass the floor
through), `DoseDao.kt`/`DoseRepository.kt`/`RoomDoseRepository.kt`/`InMemoryDoseRepository.kt`
(`latestKnownMoment`), `TrustedNowTest.kt` (+3 cases), `PurgeExpiredDoseHistoryTest.kt` (1 case
strengthened), `DoseDaoTest.kt` (+1 case), `ReminderHistoryPurgeTest.kt` (1 case split in two, +1
new case reproducing the exact scenario the review described and proving it is now closed),
`specs/dose-history-retention/spec.md` (the "Trusted-now clock guard" requirement and its first-run
scenarios updated to match).

Re-verified after the fix: `:app:testDebugUnitTest` (438/438 pass), `:app:lintDebug` (clean),
`:app:assembleDebug` (builds), `:app:compileDebugAndroidTestKotlin` (compiles clean) — same
environment and same caveat as 6.3: no emulator/device available, so the new and changed
instrumented tests remain compile-checked only.
