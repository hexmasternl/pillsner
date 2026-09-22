## Context

`ReminderCoordinator.wake()` already runs, in order, `markMissedDoses()`, `refreshPlannedDoses()`,
the due-dose posting loop, then (in `reconcileAlarms()`) `computeWakeSchedule()` and
`scheduler.reconcile()`. `ComputeWakeSchedule` arms one housekeeping `WakeMoment` at the earliest of
the next dose-lapse moment and the next daily refresh moment (00:05 local), but only computes a
daily-refresh moment at all `if (anyMedicineProducesDoses(medications))` — i.e. some active
medication currently has a schedule. `doses` (schema v5) already carries a single-column index on
`scheduled_at`, independent of the composite `(outcome, scheduled_at)` index added for pending-dose
queries.

An earlier attempt at this issue (PR #41) implemented a first version of this feature and was
closed without merging. Its review found:

1. **Critical.** Its trusted-clock reseed path, used both on first run and after a detected reboot
   (the boot clock resetting), read the raw wall clock and accepted it outright. That defeats the
   guard: reboot the device after moving its wall clock forward, and the reseed just trusts the
   tampered time.
2. Its cutoff computation captured the system default time zone once and reused it, so a later
   time-zone change could compute the calendar-year cutoff in a stale zone.
3. The housekeeping alarm's existing `anyMedicineProducesDoses` gate meant a user with no active
   scheduled medication got no daily wake at all, so their old dose history was never visited by
   the purge.
4. `ReminderCoordinator`'s purge step lacked test coverage for first wake, later wakes, and a purge
   failure, alongside confirming alarm reconciliation still completes.
5. The purge step's failure boundary caught `Exception` inside `withTimeout`, which also catches
   `TimeoutCancellationException` — silently absorbing the wake's own timeout and making a
   timed-out wake falsely report as complete instead of arming its retry
   (`reminder-scheduling`, "A wake that does not complete is retried").
6. A code comment attributed the delete's index usage to the wrong index (the composite
   `(outcome, scheduled_at)` index rather than the single-column `scheduled_at` index that actually
   serves a `WHERE scheduled_at < ?` range scan).
7. `tasks.md` marked instrumented tests as passing when they had only been compile-checked, not
   run, in an environment with no Android SDK/emulator.

This design addresses each one directly rather than carrying it forward.

`reminder-wake-cycle-db-efficiency`'s design explicitly named unbounded dose-row growth as accepted
behaviour, out of scope for that change. This change is the one that revisits that decision,
scoped exactly to what issue #30 asked for.

## Goals / Non-Goals

**Goals:**
- Delete `Dose` rows scheduled more than one calendar year in the past, as one more step of the
  existing daily housekeeping wake.
- Compute that cutoff from a wall clock that a tampered or reset system clock cannot move forward
  faster than real time actually passes, with no Android permission and no network access.
- Never let the purge or its clock guard cost the user a reminder or defeat the wake's own
  timeout/retry behaviour.
- Ensure the purge actually gets a chance to run even for a user with no currently active scheduled
  medication.
- Leave `Medication` and `Schedule` rows, and every dose still inside the rolling two-day pending
  window, completely untouched.

**Non-Goals:**
- Any user-facing setting, screen or notice about the purge. It is silent housekeeping, exactly
  like the daily window refresh it rides alongside.
- Verifying wall-clock correctness via network time (NTP or a trusted time API). Ruled out in the
  issue's own investigation: it needs `INTERNET` and an outbound call, which conflicts with this
  repository's no-network-access rule and would need its own proposal and README disclosure.
- A Room schema change. The existing single-column `scheduled_at` index already serves the delete.
- Changing what counts as pending, due or missed, or anything about the rolling two-day window
  itself (`dose-records`).

## Decisions

### D1: Retention cutoff is local calendar-date arithmetic, computed fresh each time

The cutoff is `(trusted-now's local date in the current system zone) minus one year`, at start of
day, matching the convention `medicine-usage-history` already uses for its "1 month"/"3 months"
periods. Using local dates rather than a fixed `Duration` of 365 days means month lengths, leap
days and daylight-saving transitions need no special case — the same reasoning that convention
already documents.

**Fixes review point 2 directly**: the current system zone (`ZoneId.systemDefault()`, or the zone
of an injected `Clock`) is read at the moment the cutoff is computed, never cached in a field or
constructor. `PurgeExpiredDoseHistory` takes a `Clock` (as `TrustedNow` and `ComputeWakeSchedule`
already do) rather than storing a `ZoneId` anywhere.

**Alternative considered**: a fixed 365-day (or 8760-hour) instant offset. Rejected: simpler, but
wrong at leap days and gives an inconsistent "one year" depending on which day the purge happens to
run relative to 29 February, unlike the calendar-date convention already established.

### D2: `TrustedNow` — a high-water mark anchored to the boot clock, with a safe reseed

Two longs are persisted: `trustedNowMillis` (the high-water mark, a wall-clock instant) and
`anchorElapsedRealtimeMillis` (the boot-clock reading recorded alongside it). On every observation:

1. Read the current wall clock (`currentWallMillis`) and the current boot clock
   (`currentElapsedRealtimeMillis`).
2. **No prior sample** (genuine first run — nothing persisted yet): there is no trusted anchor to
   validate the wall clock against. Seed `trustedNowMillis = currentWallMillis` and
   `anchorElapsedRealtimeMillis = currentElapsedRealtimeMillis`, persist them, and report **no
   validated trusted-now** for this observation. The caller (the purge step) skips purging this
   wake — the one case where there is genuinely nothing safe to fall back on.
3. **Boot clock has gone backward** (`currentElapsedRealtimeMillis < anchorElapsedRealtimeMillis`):
   `elapsedRealtime()` only resets to zero on reboot, so this can only mean the device rebooted
   since the last observation. The interval since the last sample cannot be verified from the wall
   clock (that is exactly the moment tampering plus a reboot would try to exploit), so
   `trustedNowMillis` is **left unchanged** — it does not advance, and it is never reseeded from the
   raw wall clock. Only `anchorElapsedRealtimeMillis` is updated, to
   `currentElapsedRealtimeMillis`, so the next observation's delta is measured from here. The
   caller still receives the (unchanged) `trustedNowMillis` as validated: it is a real value this
   device established before, so using it as the purge's basis is always safe — at worst it is
   older than the true now, which only delays a purge, never advances one early.
4. **Ordinary case** (`currentElapsedRealtimeMillis >= anchorElapsedRealtimeMillis`, no reboot
   detected): let `elapsedDelta = currentElapsedRealtimeMillis - anchorElapsedRealtimeMillis` (real
   time that has actually passed, including deep sleep) and `wallDelta = currentWallMillis -
   trustedNowMillis` (what the wall clock claims has passed since the last validated mark).
   Advance the high-water mark by `max(0, min(wallDelta, elapsedDelta))`:
   `trustedNowMillis += max(0, min(wallDelta, elapsedDelta))`, then set
   `anchorElapsedRealtimeMillis = currentElapsedRealtimeMillis`. A forward wall-clock jump
   (`wallDelta > elapsedDelta`) is capped at the real elapsed time, exactly bounding the guard's
   promise. A backward or stalled wall clock (`wallDelta <= 0`) advances the mark by zero rather
   than regressing it — the mark is a ratchet, so it only ever holds steady or moves forward.

This is the direct fix for review point 1: the earlier attempt's bug was reseeding `trustedNow`
from the raw wall clock on both the first-run *and* the reboot path. Splitting those two cases is
what closes the hole — first run has no anchor to check against (skip the purge, once, until an
anchor exists), while a reboot **does** have a previously-validated value to fall back on (freeze
it, don't discard it).

Persisted in `TrustedClockStore` (`data/reminders/`): two long preference keys, holding nothing but
epoch-millisecond timestamps — no dose id, no medicine name, no amount.

**Correction found during implementation**: the paragraph above originally said this store would be
modelled on `ArmedAlarmStore`, in device-protected storage, so it could be read before the first
unlock after a reboot. Tracing where the purge actually runs shows that is unnecessary:
`ReminderCoordinator.onWake` returns immediately, before calling into `wake()` at all, whenever
`unlockState.isUnlocked()` is false — the purge step lives inside `wake()`, so it can never run
before that guard has already passed. `TrustedClockStore` is modelled on the plain
`ReminderPreferences` DataStore instead (an ordinary, credential-encrypted preferences file, no
device-protected singleton plumbing needed), which is simpler and does not overstate what this
store has to survive.

**Correction found during PR review**: the first draft's `previous == null` branch always seeded
`trustedNowMillis` from the raw wall clock and reported it unvalidated, on the reasoning that
skipping the purge for that one observation was enough. Review (PR #50) found this incomplete: the
*seed itself* was never validated, only its future advances were — so a wall clock already
tampered forward before the very first observation would still be adopted verbatim, and the very
next ordinary observation would advance it by a small real elapsed delta and hand it back as fully
validated. The advance was correctly bounded; the baseline it advanced from was not.

There is no way to independently verify a wall-clock reading at all without a network time source,
which this feature deliberately does not have. What the app *does* have, though, is its own dose
history: a dose already stored before this observation began carries `plannedAt`, a wall-clock
reading the app itself took at write time, and could not have been written under a clock tampered
*after* it was. `TrustedNow.observe` now takes an additional `knownGoodFloorMillis` parameter — in
practice `DoseRepository.latestKnownMoment()`'s `MAX(planned_at)` — and, on a first observation,
clamps the seed to `min(currentWallMillis, knownGoodFloorMillis)` when a floor exists, validating
it immediately rather than only "eventually". Only when there is truly no floor (a fresh install
with no dose history at all) does the original skip-and-seed behavior apply — which remains
correct on its own terms, since an empty history has nothing a wrong seed could delete yet.

This does not, and cannot, close every gap: a clock tampered forward *before the very first dose is
ever recorded*, and left tampered indefinitely, leaves no independent evidence to clamp against at
all. That residual gap is recorded under Risks below rather than left implicit, since it is a
genuine limitation of a zero-network-access design, not an oversight.

**Two further corrections found in the same review round**, both about the floor not being as
independent as first claimed:

1. **The floor query itself could return a future projection.** `DoseDao.latestKnownMoment`'s first
   draft was `SELECT MAX(planned_at) FROM doses`, with no restriction on outcome. Schema v4's
   migration (`Migrations.MIGRATION_3_4`) backfills every pre-existing row's `planned_at` from its
   own `scheduled_at` — and a *pending* row's `scheduled_at` is, by design, a dose still due today
   or tomorrow, which can be later than "now". A legacy pending row surviving from before that
   migration could therefore hand the guard a floor that is itself a future value, undermining the
   whole "already happened" premise. Fixed by restricting the query to `WHERE outcome IS NOT NULL`:
   an answered dose's stored moment can never be a future projection, because nothing records an
   outcome before the moment it actually occurred. `InMemoryDoseRepository.latestKnownMoment`
   mirrors the same restriction (`!it.isPending`).
2. **The floor was read after this same wake had already written to the table.** The first draft
   had `runDoseHistoryPurge()` call `doseRepository.latestKnownMoment()` itself, at the point the
   purge step runs — which is *after* `RefreshPlannedDoses` has already run earlier in the same
   `wake()` call. `RefreshPlannedDoses.insertPlanned` writes new pending doses with
   `plannedAt = clock.instant()` — the exact same (possibly tampered) clock the guard exists to
   distrust. On a first observation with an active medication schedule (the ordinary case for any
   real user, not an edge case), those brand-new rows would become the "independent" floor,
   which is not independent of anything: the wake would be poisoning its own guard with evidence it
   just manufactured from the thing being distrusted. Fixed by reading `latestKnownMoment()` as the
   very first statement of `wake()`, before any step that writes anything, and threading that
   captured value down to `runDoseHistoryPurge(knownGoodFloor)` as a parameter rather than letting
   the purge step re-query it itself.

**Alternative considered**: reseed to the wall clock on reboot but skip the purge for that one
wake only, resuming on the next. Rejected in favour of freezing `trustedNowMillis` instead:
freezing needs no extra "skip once" flag (the frozen value is unconditionally safe to purge with,
so there is nothing to gate), and it composes correctly with an arbitrarily long chain of reboots
between wakes — each one just holds the mark at its last validated point, with no state beyond the
two longs already being persisted.

### D3: The purge runs last in the wake, in its own failure boundary that never swallows cancellation

`ReminderCoordinator.wake()` gains one more step, after the due-dose posting loop and before
returning `WakeResult`:

```kotlin
try {
    val trustedNow = trustedNow.observe()
    if (trustedNow != null) {
        val purged = doseRepository.deleteHistoryBefore(purgeExpiredDoseHistory(trustedNow))
        Log.d(TAG, "Purged $purged dose(s) older than the retention window")
    }
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Exception) {
    Log.d(TAG, "Dose history purge did not complete: ${error::class.simpleName}")
}
```

**Fixes review point 5 directly**: `CancellationException` (and its subtype
`TimeoutCancellationException`, thrown by the surrounding `withTimeout` in `runWakeBody`) is
re-thrown before the generic `catch (error: Exception)` can see it, so a wake that times out while
the purge is running is still reported as `WakeOutcome.TimedOut` and retried, per
`reminder-scheduling`'s existing "A wake that does not complete is retried" requirement. Only a
genuine purge failure (a Room error, for instance) is logged and swallowed, exactly like the
existing due-dose and refresh steps' error handling elsewhere in `wake()`.

Running last, after reminders have already been posted and outcomes already recorded, means the
purge can never delay or block anything the user is waiting on; its own failure changes nothing
about the rest of the wake, which has already completed by the time it runs.

**Correction found in the same review round**: the same `runCatching`-swallows-cancellation class
of bug was also present one level up, in `reconcileAlarms()` — unrelated to the purge step itself,
but introduced by this change all the same, since `reconcileAlarms()` now makes a new suspending
call (`doseRepository.hasAnyDose()`, for D5's `hasDoseHistory` gate) that a cancellation could land
inside of. Its `runCatching { ... }.getOrNull()` caught every `Throwable`, cancellation included,
which would let an external cancellation of the coroutine `reconcileAlarms()` runs in look like an
ordinary "could not compute the schedule" failure instead of propagating. Fixed the same way as the
purge step: an explicit `try`/`catch` that re-throws `CancellationException` before the generic
`catch (error: Exception)`.

### D4: `DoseRepository.deleteHistoryBefore` — one indexed delete, no schema change

```kotlin
// DoseRepository
suspend fun deleteHistoryBefore(cutoff: Instant): Int

// DoseDao
@Query("DELETE FROM doses WHERE scheduled_at < :cutoff")
suspend fun deleteHistoryBefore(cutoff: Instant): Int
```

Served by the existing `Index(value = ["scheduled_at"])` on `DoseEntity` — a single-column index
that supports a `WHERE scheduled_at < ?` range scan directly. **Fixes review point 6**: this is
*not* the composite `(outcome, scheduled_at)` index added by `reminder-wake-cycle-db-efficiency`
(that index serves `WHERE outcome IS NULL` lookups and cannot efficiently serve a range scan on its
non-leading column). No column changes and no new index, so no Room migration and no schema version
bump.

Every row this deletes already carries a non-null `outcome`: the rolling two-day window
(`dose-records`, "Rolling two-day window") means nothing pending can be a year old, so this can
never remove a dose the user still has to answer. `RoomDoseRepository`, `InMemoryDoseRepository`
and `DoseDao`'s own "production never removes a dose" comment are updated to describe this new,
narrow exception rather than silently going stale.

### D5: Housekeeping alarm arms whenever there is dose history to purge, not only when a medicine is scheduled

**Fixes review point 3 directly.** `ComputeWakeSchedule.anyMedicineProducesDoses(medications)`
currently gates the entire daily-refresh housekeeping moment. Once the housekeeping wake also
carries a purge duty, that gate is too narrow: a user with no active or scheduled medication (all
deactivated, or removed down to none) never gets a daily wake at all, so any dose history they
still have is never purged.

`ComputeWakeSchedule` gains a second input, `hasDoseHistory: Boolean`, alongside the existing
`snapshot` and `medications`, supplied by the caller (`ReminderCoordinator`, from a new
`DoseRepository.hasAnyDose(): Boolean`, backed by `SELECT EXISTS(SELECT 1 FROM doses LIMIT 1)` — a
cheap existence check, not a count). The daily-refresh housekeeping moment is now computed
`if (anyMedicineProducesDoses(medications) || hasDoseHistory)`. In the overwhelmingly common case
(any dose has ever been recorded) this evaluates to `true` immediately and changes nothing
observable about *when* the alarm fires — it only stops the alarm from being withheld entirely in
the one edge case that matters here.

This is a real, if narrow, behavioural change to `reminder-scheduling`'s "Daily refresh"
requirement, which is why it is listed as a modified capability rather than folded silently into
`dose-history-retention`'s own spec.

**Alternative considered**: add a second, independent alarm/trigger dedicated to the purge.
Rejected: the issue itself asks for "one more idempotent step in that existing wake cycle rather
than introducing a second background mechanism," and a second alarm is exactly that.

## Risks / Trade-offs

- **[Risk] The trusted-clock guard adds a new persisted state that could itself drift or corrupt.**
  → Mitigation: only two longs, in the same DataStore pattern as the already-shipped
  `ReminderPreferences`; a corrupt or unreadable value is treated as "no prior sample" (case 2
  above), which safely falls back to the dose-history floor when one exists, or re-seeds and skips
  one purge when it doesn't, rather than failing in an unsafe direction.
- **[Risk] A clock tampered forward before the very first dose is ever recorded, and left tampered
  indefinitely, leaves no independent evidence for the seed-clamping fix (D2) to draw on.** →
  Accepted, and stated plainly rather than implied: this is a genuine limit of a zero-network-access
  design, not an oversight (see D2's "Correction found during PR review"). It requires the clock to
  already be wrong *before the app ever writes a single dose*, and stay wrong forever after; the
  moment any real dose history exists — which for an actual user happens almost immediately — the
  clamp fully protects it from that point forward. Nothing a user has ever seen the app record
  under a consistently tampered clock is inconsistent with what the app deletes under that same
  clock, so no data the user perceived as real is lost.
- **[Risk] Freezing `trustedNowMillis` across a reboot could delay a purge indefinitely if the
  device reboots very frequently.** → Accepted: each reboot only costs the *delta since the last
  sample*, not the whole history; ordinary elapsed time between reboots (which still counts,
  including deep sleep) keeps accumulating via `elapsedRealtime()`'s own definition. A device
  rebooting often enough to matter is already an unusual, low-risk scenario for this feature (it
  only delays deletion of stale data, it never loses anything).
- **[Risk] `ComputeWakeSchedule`'s new `hasDoseHistory` parameter is one more thing every caller
  must supply correctly.** → Mitigation: `ReminderCoordinator` already builds a `PendingSnapshot`
  and reads the medication list once per wake; adding one more cheap existence read alongside them
  keeps the wiring localized to the one call site that constructs `ComputeWakeSchedule`'s inputs.
  Existing `ComputeWakeScheduleTest` cases are ported with an explicit `hasDoseHistory` argument
  rather than left to default silently, so every scenario states its assumption.
- **[Risk] Instrumented tests for `TrustedClockStore` and the new `DoseDao` query cannot run in
  this environment (no Android SDK/emulator).** → Mitigation, and fix for review point 7: `tasks.md`
  states plainly whether each instrumented test was executed or only compile-checked, and a manual
  test case covers the reboot/clock-jump scenarios that most need a real device.

## Migration Plan

No Room migration (D4). Ship as one change on `feature/dose-history-retention`, cut from
`development`, in the order `tasks.md` lays out: domain (`TrustedNow`,
`PurgeExpiredDoseHistory`) and its persistence (`TrustedClockStore`) first since nothing else
depends on them, then the repository/DAO delete method, then `ComputeWakeSchedule`'s widened
condition, then wiring the purge step into `ReminderCoordinator.wake()` last, since it is the one
step that touches the fragile wake cycle. Rollback is a normal revert; there is no data
transformation to undo, only a delete this change introduces — a revert simply stops the app from
issuing it again.

## Open Questions

None outstanding. If a future change wants the retention window to be configurable, that is a
separate proposal — this one implements exactly the one-year, non-configurable policy issue #30
asked for.
