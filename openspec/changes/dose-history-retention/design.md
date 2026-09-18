## Context

The `doses` table (schema v5) grows without bound today: every taken, skipped or missed dose stays forever, even though the Usage history screen (`medicine-usage-history`) never shows anything past 3 months. The app already runs a daily housekeeping wake — `ReminderCoordinator.wake()`, armed by `ReminderAlarmScheduler` at "the earliest of the next dose lapse moment and the next daily refresh moment" (`reminder-scheduling`, "Set of armed alarms") — which extends the rolling 2-day dose window once a day near local midnight. That is the natural place to add one more idempotent step rather than introducing a second background mechanism.

GitHub issue #30 raises a specific risk: a device clock set forward (by accident, by the user, or by tampering) would make the purge think recent history is over a year old and delete it prematurely. A clock set backward is not a data-loss risk for this feature — it only makes the purge run later than it ideally would.

## Goals / Non-Goals

**Goals:**
- Delete dose rows whose scheduled moment is more than 1 year in the past, by local calendar date, as a step in the existing wake cycle.
- Protect the purge from a forward-tampered wall clock, using only on-device signals — no network, no new Android permission.
- Never touch `Medication` or `Schedule` rows, and never touch a pending dose (which cannot reach this age by design — see `dose-records`, "Rolling two-day window").
- Keep the retention cutoff's date arithmetic consistent with the convention `medicine-usage-history` already established for its periods (local calendar dates, not fixed-duration instant offsets), so leap days and DST need no special handling.

**Non-Goals:**
- Detecting or correcting the *displayed* clock/time zone elsewhere in the app — this only affects the retention purge's own notion of "now".
- Any server-verified or network-verified time source. Ruled out: it would need `INTERNET` and a network call, which conflicts with this project's no-network-access privacy rule (`CLAUDE.md`) and would need its own proposal and README disclosure.
- Exporting or archiving history before it is deleted. Issue #30 asks for removal, not export.
- Changing what the Usage history screen shows (still capped at 3 months) or its own spec.

## Decisions

### D1: The purge runs inside `ReminderCoordinator.wake()`, not a new mechanism
Every wake already reconciles alarms and runs idempotent steps (`markMissedDoses`, `refreshPlannedDoses`). Adding a `PurgeExpiredDoseHistory` step there means: no `WorkManager`, no new alarm type, no new permission, and it inherits the existing wake budget, retry and logging behaviour for free. The step is cheap (a single indexed `DELETE`), so running it on every wake — not just the once-daily housekeeping wake — is acceptable and keeps the step itself simple (no "have I run today yet" bookkeeping to get wrong).

*Alternative considered:* a dedicated `WorkManager` periodic job. Rejected — `CLAUDE.md` already steers away from periodic background work for the app's core timing, and a second scheduling mechanism just for this is unjustified complexity when the wake cycle already runs at least daily.

### D2: Retention cutoff is a local calendar date, not a fixed instant offset
"One year ago" is computed as `LocalDate.now(trustedClock).minusYears(1)` in the device zone, then converted to the start of that local day as the delete boundary — the same approach `medicine-usage-history`'s "The window a period covers" requirement already uses for "1 month"/"3 months". This means 29 February is handled the same way `LocalDate.minusYears` already handles it (resolves to 28 February on a non-leap target year), with no bespoke leap-day logic to write or test beyond confirming that behaviour.

### D3: Trusted-now is a boot-clock-anchored high-water mark, persisted outside Room
A new small DataStore-backed store (`TrustedClockStore`, modelled on `ArmedAlarmStore`) persists two longs: the last observed wall-clock instant and the last observed `SystemClock.elapsedRealtime()` reading. On each wake:

1. Read the persisted pair (`lastWall`, `lastBoot`), or treat this as the first-ever run if none is stored.
2. Read the current wall clock (`now`) and boot clock (`bootNow`).
3. `bootDelta = bootNow - lastBoot` (real elapsed time; ≥ 0 always, but may be *smaller* than expected after a reboot, since `elapsedRealtime` resets to 0 at boot — see Risks).
4. `trustedNow = lastWall + min(now - lastWall, bootDelta)` — the wall clock is trusted to advance, but only by as much as the boot clock proves actually elapsed. A backward wall-clock move (`now - lastWall` negative) is never a problem for this formula: retention only ever needs a lower bound on "how much time has passed," and this deliberately never lets that bound decrease.

   Restated: `trustedNow = lastWall + max(0, min(now - lastWall, bootDelta))`.
5. Persist `(trustedNow, bootNow)` as the new `(lastWall, lastBoot)` pair.
6. The purge uses `trustedNow` (not the raw `now`) as the basis for D2's "one year ago" calculation.

First-ever run (no persisted pair): seed `lastWall = now`, `lastBoot = bootNow`, and skip the purge for that wake (nothing to compare against yet; the very next wake, at least a day later in the ordinary case, has a real baseline).

*Alternative considered:* comparing wall-clock deltas between wakes with no boot-clock cross-check. Rejected — this cannot distinguish "the user waited a year" from "the user set the clock forward a year," which is exactly the case #30 asks to guard against.

*Alternative considered:* refusing to purge at all when *any* wall clock/boot clock mismatch is seen. Rejected — this would also misfire on an ordinary reboot (D-notes below) and give up protection entirely rather than degrading gracefully; capping the advance is strictly better; it costs nothing extra to implement.

### D4: New `DoseRepository` method, no new Room schema version
Add `suspend fun deleteHistoryBefore(cutoff: Instant): Int` to `DoseRepository`, backed by a single `DELETE FROM doses WHERE scheduled_at < :cutoff` (Room DAO). `doses` already has an index on `scheduled_at` (schema v5's composite index on `(outcome, scheduled_at)`), so this is an index range scan, not a table scan. No column changes, so no new schema version or migration is needed.

## Risks / Trade-offs

- **[Risk]** A full power-off (not just sleep) resets `elapsedRealtime()` to 0 on the next boot, so `bootDelta` computed against the pre-reboot `lastBoot` would be nonsensical (very large negative, since `bootNow < lastBoot`) → **Mitigation:** treat `bootNow < lastBoot` as "the boot clock cannot be compared" and fall back to seeding fresh from this wake (same as first-ever run), skipping the purge for this one wake. Worst case: one extra wake's delay before the trusted clock re-establishes itself, never an incorrect purge.
- **[Risk]** A device left off for a genuinely long time (e.g. in a drawer for a year) will, after the reboot-triggered reseed above, need real elapsed time to accumulate again before the trusted clock "believes" that a year has passed for any dose recorded near that boundary → **Mitigation:** this only delays deletion of history that is already borderline; it never deletes something prematurely, which is the risk this design exists to prevent. Accepted as the deliberately safe direction.
- **[Risk]** The DataStore holding `TrustedClockStore` could itself be cleared (e.g. "clear app data") → **Mitigation:** this is equivalent to first-ever run; the purge simply reseeds and waits for the next wake. No sensitive data is at risk since the store holds only two timestamps, no medicine data.
- **[Trade-off]** This guard adds a small amount of state and logic for what is a low-severity feature (stale history, not safety- or money-critical). Accepted per the explicit decision on issue #30 to include it now rather than defer it.

## Migration Plan

No Room migration is needed (no schema version change). Deployment is a normal app update:
- First wake after update: `TrustedClockStore` has no persisted pair, so it seeds and skips the purge once, exactly like the documented first-run behaviour.
- No rollback concerns beyond an ordinary app rollback — the new `DoseRepository` method and `TrustedClockStore` are additive; no existing table or column is changed.

## Open Questions

None outstanding — scope, mechanism and persistence were resolved during proposal review (see issue #30 discussion).
