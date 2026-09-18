## 1. Domain: retention cutoff and purge use case

- [x] 1.1 Add a `PurgeExpiredDoseHistory` use case in `domain/scheduling` that takes a trusted-now `Instant` and a `Clock`/`ZoneId`, computes the retention cutoff as one local calendar year back (start of that local day), and calls `DoseRepository.deleteHistoryBefore(cutoff)`.
- [x] 1.2 Unit test the cutoff arithmetic in isolation: an ordinary year, a cutoff that lands on 29 February (leap year on one side only), and a year that spans one or more daylight-saving transitions — no Android dependency, per `CLAUDE.md` testing expectations.

## 2. Domain: trusted-now guard

- [x] 2.1 Define a small domain-level `TrustedNow` calculation (pure function) that, given the previous `(lastWall, lastBoot)` sample and the current `(now, bootNow)` reading, returns the new trusted-now instant per design D3: `trustedNow = lastWall + max(0, min(now - lastWall, bootNow - lastBoot))`, and detects the "no prior sample" and "boot clock went backward (reboot)" cases as a reseed.
- [x] 2.2 Unit test: ordinary elapsed time advances trusted-now normally; a wall-clock forward jump is capped to the boot-clock delta; a wall-clock backward move does not move trusted-now backward and does not block anything; a reboot (boot clock lower than the recorded sample) triggers a reseed instead of a nonsensical delta; no prior sample triggers a reseed.

## 3. Data: persistence

- [x] 3.1 Add `TrustedClockStore`, a small DataStore-backed store (modelled on `data/reminders/ArmedAlarmStore.kt`) that persists the `(lastWall, lastBoot)` pair as two longs, with `read()` and `write(lastWall, lastBoot)`.
- [x] 3.2 Add `DoseRepository.deleteHistoryBefore(cutoff: Instant): Int` to the domain repository contract.
- [x] 3.3 Implement it on the Room-backed repository with a new DAO method: `DELETE FROM doses WHERE scheduled_at < :cutoff`. No schema version change (uses the existing schema v5 index on `scheduled_at`).
- [x] 3.4 DAO test: rows before the cutoff are deleted, rows on or after the cutoff are kept, a pending row (no outcome, always after the cutoff in practice) is unaffected, and running the delete twice in a row is a no-op the second time.

## 4. Wiring: the wake cycle

- [x] 4.1 In `ReminderCoordinator.wake()`, add the purge as one more step: read/update `TrustedClockStore`, compute trusted-now via the task 2.1 function, and — unless this wake is a reseed (task 2.2) — run `PurgeExpiredDoseHistory` with it. Keep the step idempotent and non-fatal (a failure here must not stop the rest of the wake, matching how other steps in `runWakeBody` already tolerate exceptions).
- [x] 4.2 Wire `TrustedClockStore` and `PurgeExpiredDoseHistory` into `AppContainer`, following the existing pattern for `MarkMissedDoses`/`RefreshPlannedDoses`.
- [x] 4.3 Confirm nothing here logs a medicine name, dose amount, or any wall-clock/boot-clock value at or above info level in release builds — only dose ids and counts, per `CLAUDE.md`'s logging rule and the pattern already followed in `ReminderCoordinator`.

## 5. Verification

- [x] 5.1 Run the unit test task and confirm the new domain and DAO tests pass alongside the existing suite.
- [x] 5.2 Run lint and confirm no new violations.
- [x] 5.3 Manual test case (documented here since it needs real device/emulator clock changes, per `CLAUDE.md`'s expectation that reboot/clock-change/DST scenarios be covered by a test or a documented manual case): set the emulator's date forward by more than a year, wake the app (e.g. open it), and confirm dose history from before the jump is **not** deleted on that wake; advance real time slightly and confirm it still is not deleted until genuine elapsed time — not the jumped wall clock — reaches the cutoff.

  **Documented manual test case (not run — this environment has no Android SDK/emulator at all,
  so the instrumented tests below are confirmed to compile but were never executed on a device):**
  1. Install a debug build on an emulator or device with a medicine that has at least one dose
     recorded (taken, skipped or missed) scheduled more than a year before "today".
  2. Open the app once so a first wake seeds `TrustedClockStore` (this wake never purges — it is
     the "no prior sample" reseed case).
  3. Force-stop and reopen the app (or wait for the next housekeeping wake) so a second wake runs
     with a real baseline. Confirm the old dose row is now deleted (`adb shell` query against the
     app's database, or the Usage history screen showing nothing there, is enough to confirm).
  4. Reinstall/reset, record a dose again more than a year back, open the app once to seed the
     baseline, then use the emulator's clock settings (Settings > System > Date & time, with
     "Automatic date & time" off) to set the wall clock forward by more than a year.
  5. Reopen the app (or trigger a wake). Confirm the old dose row is **not** deleted on this wake —
     the wall clock jump is capped by the boot clock's real elapsed time, which is only seconds.
  6. Leave the emulator running for a few real minutes (do not change the wall clock further), then
     trigger another wake. Confirm the dose is still not deleted: only a few real minutes have
     elapsed, nowhere near the year the retention rule requires.
  7. As an additional reboot check: restart the emulator, reopen the app, and confirm this wake
     also skips the purge (the boot clock reset to near zero triggers the reseed case), with the
     next wake after that purging normally from the fresh baseline.
