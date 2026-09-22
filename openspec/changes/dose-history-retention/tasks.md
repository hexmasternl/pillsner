## 1. Trusted clock

- [ ] 1.1 Add `TrustedNow` (`domain/scheduling/`), a pure function/class that, given the persisted
  `(trustedNowMillis, anchorElapsedRealtimeMillis)` sample (or none), the current wall clock and
  the current boot-clock (`elapsedRealtime`) reading, returns the updated sample to persist plus
  whether trusted-now is validated for use this observation — implementing design.md D2 exactly:
  no-prior-sample seeds and reports unvalidated; boot-clock-went-backward freezes the mark and
  reseeds only the anchor; the ordinary case advances the mark by
  `max(0, min(wallDelta, elapsedDelta))`. No Android framework dependency.
- [ ] 1.2 Add `TrustedClockStore` (`data/reminders/`), a DataStore-backed store for the two longs,
  modelled on `ArmedAlarmStore`: same device-protected-storage pattern, same "nothing identifying
  is ever written here" discipline, its own preferences file.
- [ ] 1.3 Unit test `TrustedNow` covering: ordinary elapsed time: forward clock jump capped at the
  real elapsed delta; backward clock move does not regress the mark; boot clock going backward
  (reboot) freezes the mark and does not reseed from the wall clock; no prior sample seeds and
  reports unvalidated; a later observation after a seed advances normally and reports validated.

## 2. Purge domain logic

- [ ] 2.1 Add `PurgeExpiredDoseHistory` (`domain/scheduling/`), a pure function taking a validated
  trusted-now instant and a `Clock` (for `Clock.zone`, read fresh every call — never cached), that
  returns the one-year-ago local-calendar-date cutoff as an `Instant`.
- [ ] 2.2 Unit test `PurgeExpiredDoseHistory` covering: an ordinary one-year cutoff; the leap-day
  boundary (29 February); a cutoff that spans a daylight-saving transition; the current system zone
  being read at call time, not cached (e.g. two calls with different injected zones on the same
  `Clock` produce different cutoffs).

## 3. Repository and DAO

- [ ] 3.1 Add `DoseDao.deleteHistoryBefore(cutoff: Instant): Int` —
  `DELETE FROM doses WHERE scheduled_at < :cutoff` — and `DoseDao.hasAnyDose(): Boolean` —
  `SELECT EXISTS(SELECT 1 FROM doses LIMIT 1)`. No entity, index or schema version change.
- [ ] 3.2 Add `DoseRepository.deleteHistoryBefore(cutoff: Instant): Int` and
  `DoseRepository.hasAnyDose(): Boolean` to the domain interface; implement both in
  `RoomDoseRepository` (delegating to the DAO) and `InMemoryDoseRepository` (delegating to its
  in-memory store).
- [ ] 3.3 Update `DoseDao`'s "production never removes a dose" comment and
  `DoseRepository.observeHistoryFor`'s "history is never withdrawn" KDoc to describe this purge as
  the one narrow, documented exception, so neither comment goes stale.
- [ ] 3.4 DAO test: seed doses older and newer than a cutoff, call `deleteHistoryBefore`, confirm
  only the older ones are removed and the return value matches the count deleted; a dose with no
  outcome (pending) scheduled before the cutoff is also removed if seeded directly (documenting
  that the query itself does not special-case outcome — it relies on the rolling two-day window
  ensuring this never happens in practice, per `dose-history-retention`'s spec).
- [ ] 3.5 DAO test for `hasAnyDose()`: false on an empty table, true once any dose exists, false
  again after `deleteAll()`.

## 4. Housekeeping alarm gate

- [ ] 4.1 Add a `hasDoseHistory: Boolean` parameter to `ComputeWakeSchedule.invoke(...)`; widen the
  daily-refresh condition to `if (anyMedicineProducesDoses(medications) || hasDoseHistory)`.
- [ ] 4.2 Update every existing `ComputeWakeScheduleTest` call site to pass an explicit
  `hasDoseHistory` argument (matching the scenario's intent) rather than relying on a default.
- [ ] 4.3 Add `ComputeWakeScheduleTest` cases for: no active medication and no dose history (no
  daily-refresh moment armed on that account); no active medication but `hasDoseHistory = true`
  (daily-refresh moment is armed); an active scheduled medication with `hasDoseHistory = false`
  (unchanged existing behaviour).

## 5. Wiring the purge into the wake cycle

- [ ] 5.1 In `ReminderCoordinator.wake()`, after the due-dose posting loop and before building the
  final `WakeResult`, add the purge step exactly as design.md D3 specifies: observe trusted-now,
  and only if validated, compute the cutoff and call `deleteHistoryBefore`; wrap in a
  `try { ... } catch (c: CancellationException) { throw c } catch (e: Exception) { log }` so a
  wake timeout is never swallowed.
- [ ] 5.2 Pass `doseRepository.hasAnyDose()` into `computeWakeSchedule(...)` in `reconcileAlarms()`
  alongside the existing snapshot and medication list.
- [ ] 5.3 Wire `TrustedNow`, `TrustedClockStore` and `PurgeExpiredDoseHistory` into
  `AppContainer`, and thread `hasAnyDose()`'s repository call through wherever `ComputeWakeSchedule`
  is constructed/invoked.
- [ ] 5.4 `ReminderCoordinator` test coverage for the purge step: a first wake (no persisted
  trusted-clock sample) seeds the clock and skips the purge but still completes normally; a
  subsequent wake with a validated trusted-now runs the purge; a purge that throws is logged and
  does not prevent due doses from being posted or the alarm set from being reconciled; a wake that
  times out while the purge step is running is reported as `TimedOut` (retried), not `Completed`.

## 6. Verification

- [ ] 6.1 Run the unit test task (`:app:testDebugUnitTest`) and confirm every new and existing test
  passes.
- [ ] 6.2 Run the lint task (`:app:lintDebug`) and confirm no new warnings or errors.
- [ ] 6.3 Run the instrumented test task if an Android SDK/emulator is available in this
  environment; if it is not, say so explicitly here rather than marking these tests as passed, and
  record whether they were at least compile-checked (`:app:compileDebugAndroidTestKotlin`).
- [ ] 6.4 Add/extend `DoseDaoTest` (task 3.4, 3.5) and a `TrustedClockStoreTest` (task 1.2's
  persistence, read-after-write and corrupt/missing-value-treated-as-no-prior-sample behaviour) to
  the instrumented test suite.
- [ ] 6.5 Document a manual test case for the clock-tamper scenarios that cannot be automated on
  this environment: (a) advance the device's wall clock forward by more than a year, force a wake,
  and confirm no dose younger than a year (by real time) was purged; (b) reboot the device after
  (a) and confirm the guard still does not purge based on the tampered time; (c) let real time
  actually pass (or set the clock back to correct) and confirm the purge resumes normally once
  trusted-now catches up.
- [ ] 6.6 Record verification results in this section, including anything that could not be run
  here and what a reviewer with a device/emulator should re-check.
