## 1. Scaffold and dependencies

- [ ] 1.1 Verify the project scaffold exists (Gradle project at `src`, wrapper, version catalog, `app` module, manifest, `MainActivity`, `AppContainer`, theme) and stop if it does not; this change creates none of it
- [ ] 1.2 Add `work` version and `androidx-work-runtime-ktx` library to `src/gradle/libs.versions.toml`, newest stable, never a pre-release
- [ ] 1.3 Add the dependency to `src/app/build.gradle.kts` and confirm the debug assembly still builds
- [ ] 1.4 Mirror the new dependency version in the README Toolchain table

## 2. Dose repeat state (design D5)

- [ ] 2.1 Add a `reminderCount` column (integer, default 0) to the dose entity and bump the Room schema version
- [ ] 2.2 Write the migration and its migration test; do not enable destructive fallback
- [ ] 2.3 Expose `reminderCount` on the `Dose` domain model and add the repository methods to read and increment it
- [ ] 2.4 Add DAO tests for the new column and the increment

## 3. Alarm scheduler: per-dose alarms at the alarm-clock tier (design D1, D2)

- [ ] 3.1 Replace `ComputeNextWake` with `ComputeWakeSchedule` returning the full set of wake moments: every un-reminded pending dose's `scheduledAt`, every outstanding snooze end, every outstanding repeat moment, and one housekeeping moment (earliest of next lapse and next daily refresh)
- [ ] 3.2 Unit-test `ComputeWakeSchedule` for: two doses today, snooze earliest, repeat pending, nothing to remind, quiet day with only the daily refresh
- [ ] 3.3 Rework `ReminderAlarmScheduler` to arm a set of alarms, with the `PendingIntent` request code derived from the dose id and the alarm kind, and cancel the fixed request code 1 on first reconcile so no stale single alarm survives
- [ ] 3.4 Use `AlarmManager.setAlarmClock` for dose, snooze and repeat alarms; keep `setExactAndAllowWhileIdle` for the housekeeping alarm; keep the ten-minute-window fallback when exact alarms are not permitted
- [ ] 3.5 Record the armed alarm set (moments, dose ids and kinds only, no medicine name or amount) in a device-protected DataStore file, so a reconcile and the watchdog can both tell what is missing
- [ ] 3.6 Add a `reconcile(schedule)` entry point that cancels alarms no longer wanted and arms those not yet set, and make it idempotent
- [ ] 3.7 Unit-test the reconcile: adding a dose, answering a dose, deactivating a medicine, and running it twice changing nothing

## 4. Wake processing in a short foreground service (design D4)

- [ ] 4.1 Add a low-importance `wake` notification channel and its strings for the service's own notification
- [ ] 4.2 Create `ReminderWakeService`, a foreground service with `foregroundServiceType="shortService"`, taking a `WakeReason` and running the wake cycle, then stopping itself
- [ ] 4.3 Declare the service in the manifest, `directBootAware`, and add `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SHORT_SERVICE` permissions
- [ ] 4.4 Make `ReminderAlarmReceiver` and `SystemEventsReceiver` thin: start the service rather than doing the work in `goAsync()`
- [ ] 4.5 Move the action handling in `ReminderActionReceiver` to the same service where it cannot be guaranteed to finish in the receiver budget
- [ ] 4.6 Verify the service stays well inside the `shortService` cap and stops itself; instrument the cold-start path where the database has to be opened

## 5. No silent loss (design D7)

- [ ] 5.1 Make `ReminderNotifier.show` return whether it actually posted, and have `post` report a refused `notify`
- [ ] 5.2 In `ReminderCoordinator`, write `setFirstReminded` only when the notification was posted
- [ ] 5.3 Move `rescheduleNextWake` out of the blanket `finally` into a path that knows whether the wake body completed, keeping "reconcile even on failure" true
- [ ] 5.4 On a wake that times out, arm a retry two minutes out, bounded at three attempts, after which the ordinary lapse rule applies
- [ ] 5.5 Unit-test: permission absent leaves the dose un-reminded; permission granted later still produces the reminder; a timed-out wake arms a retry and does not lapse the dose; retries exhaust

## 6. Repeating reminders (design D5)

- [ ] 6.1 Extend `DueDoses` to return doses whose repeat moment has arrived, bounded at four repeats and never at or after the lapse moment
- [ ] 6.2 Increment `reminderCount` where a repeat is posted, and reset it to zero when the user snoozes
- [ ] 6.3 Add `setFullScreenIntent` to the due-dose notification, degrading to a heads-up notification when the capability is not granted; add `USE_FULL_SCREEN_INTENT` to the manifest
- [ ] 6.4 Unit-test the repeat rule: repeats at 15-minute steps, capped at four, stopped by any answer, never past lapse, reset by snooze

## 7. Battery optimisation and the Home banner (design D6)

- [ ] 7.1 Add `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` to the manifest and a source that reports `PowerManager.isIgnoringBatteryOptimizations`
- [ ] 7.2 Request the exemption once, when the first active medicine with a schedule is saved, remembering in `ReminderPreferences` that it has been asked
- [ ] 7.3 Add the battery state to `HomeUiState` and `HomeViewModel`, and implement the banner precedence: notifications denied, then battery not exempt, then alarms inexact, at most one banner shown
- [ ] 7.4 Add the banner strings for the battery state and its button, in every supported language
- [ ] 7.5 Add the vendor auto-start table matched on `Build.MANUFACTURER`, each intent guarded by `resolveActivity`, falling back to the generic system setting when none resolves
- [ ] 7.6 Unit-test the banner precedence and the vendor fallback; add a semantics test for the banner on Home
- [ ] 7.7 Run the `pillsner-ui-review` skill over the banner changes before calling this group done

## 8. Watchdog (design D3)

- [ ] 8.1 Create the periodic worker: recompute the schedule, compare against the recorded armed set, re-arm what is missing, then run a normal wake
- [ ] 8.2 Return immediately when no active medicine produces doses
- [ ] 8.3 Enqueue it as unique periodic work with `ExistingPeriodicWorkPolicy.KEEP` from `PillsnerApplication.onCreate` and from the boot receiver
- [ ] 8.4 Test: alarms intact does nothing; a dropped alarm is re-armed and the due dose posted; no alarms at all are re-armed from scratch; no medicines ends immediately
- [ ] 8.5 Confirm no medicine name or amount is logged anywhere in the worker or the scheduler, and dose ids only at debug level

## 9. Direct boot and unlock (design D8)

- [ ] 9.1 Add `LOCKED_BOOT_COMPLETED` and `ACTION_USER_UNLOCKED` to `SystemEventsReceiver` and its manifest intent filter, with new `WakeReason` values
- [ ] 9.2 Mark the alarm receiver, the system events receiver and the wake service `directBootAware`
- [ ] 9.3 On locked boot, re-arm from the device-protected armed-alarm store only; do not touch Room or credential-encrypted DataStore
- [ ] 9.4 On user unlock, run a full wake and reconcile
- [ ] 9.5 Instrumented test for re-arming before first unlock and the full wake after it

## 10. Wiring

- [ ] 10.1 Register the new scheduler, service entry point, watchdog and battery-optimisation source in `AppContainer`, keeping construction cheap and side-effect free
- [ ] 10.2 Confirm `PillsnerApplication.onCreate` still starts quickly: the wake goes to the service, and the two `runBlocking` DataStore reads are not joined by a third

## 11. Verification

- [ ] 11.1 Run the unit test task from `src` and report any failure verbatim
- [ ] 11.2 Run the lint task from `src` and clear anything it raises
- [ ] 11.3 Run the instrumented tests: alarm scheduling, notification posting, database migration, boot and unlock
- [ ] 11.4 Manual test, documented in the change: schedule a dose a few minutes out, background the app, make a phone call across the due moment, confirm the reminder arrives within the minute
- [ ] 11.5 Manual test: force-stop nothing, leave the device idle overnight with a morning dose, confirm the reminder arrives
- [ ] 11.6 Manual test: reboot across a due dose without unlocking, confirm the reminder arrives
- [ ] 11.7 Manual test: deny the battery exemption, confirm the banner appears and its button opens the right system screen

## 12. Documentation

- [ ] 12.1 Update the README: the status-bar alarm icon a scheduled medicine now causes, the battery-optimisation request and why the app asks for it, and the new permissions
- [ ] 12.2 Record the Play Console declaration text for `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, citing the exact-alarm core function
- [ ] 12.3 Confirm no network permission, third-party SDK or telemetry was added
