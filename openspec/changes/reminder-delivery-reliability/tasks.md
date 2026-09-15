## 1. Prerequisites and dependencies

- [ ] 1.0 Confirm `reminder-delivery-after-reboot` has been implemented and archived; this change builds on the device-protected alarm store, the record-when-posted rule, the retry rule and the locked-wake guard it introduces, and MUST NOT reimplement any of them
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
- [ ] 3.5 Extend the device-protected store from `reminder-delivery-after-reboot` from one moment to the armed set (moments and alarm kinds only, no medicine name, amount or dose id), so a reconcile, a locked boot and the watchdog can all tell what is missing
- [ ] 3.6 Add a `reconcile(schedule)` entry point that cancels alarms no longer wanted and arms those not yet set, and make it idempotent
- [ ] 3.7 Unit-test the reconcile: adding a dose, answering a dose, deactivating a medicine, and running it twice changing nothing

## 4. Wake processing in a short foreground service (design D4)

- [ ] 4.1 Add a low-importance `wake` notification channel and its strings for the service's own notification
- [ ] 4.2 Create `ReminderWakeService`, a foreground service with `foregroundServiceType="shortService"`, taking a `WakeReason` and running the wake cycle, then stopping itself
- [ ] 4.3 Declare the service in the manifest, `directBootAware`, and add `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SHORT_SERVICE` permissions
- [ ] 4.4 Make `ReminderAlarmReceiver` and `SystemEventsReceiver` thin: start the service rather than doing the work in `goAsync()`, keeping the receivers `directBootAware` and keeping the locked-boot branch database-free
- [ ] 4.4a Keep the locked-wake guard and the retry from `reminder-delivery-after-reboot` in place inside the service; the service makes a timeout rare but is not a reason to remove the backstop
- [ ] 4.5 Move the action handling in `ReminderActionReceiver` to the same service where it cannot be guaranteed to finish in the receiver budget
- [ ] 4.6 Verify the service stays well inside the `shortService` cap and stops itself; instrument the cold-start path where the database has to be opened

## 5. Repeating reminders (design D5)

- [ ] 5.1 Extend `DueDoses` to return doses whose repeat moment has arrived, bounded at four repeats and never at or after the lapse moment
- [ ] 5.2 Increment `reminderCount` where a repeat is posted, and reset it to zero when the user snoozes
- [ ] 5.3 Add `setFullScreenIntent` to the due-dose notification, degrading to a heads-up notification when the capability is not granted; add `USE_FULL_SCREEN_INTENT` to the manifest
- [ ] 5.4 Unit-test the repeat rule: repeats at 15-minute steps, capped at four, stopped by any answer, never past lapse, reset by snooze

## 6. Battery optimisation and the Home banner (design D6)

- [ ] 6.1 Add `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` to the manifest and a source that reports `PowerManager.isIgnoringBatteryOptimizations`
- [ ] 6.2 Request the exemption once, when the first active medicine with a schedule is saved, remembering in `ReminderPreferences` that it has been asked
- [ ] 6.3 Add the battery state to `HomeUiState` and `HomeViewModel`, and implement the banner precedence: notifications denied, then battery not exempt, then alarms inexact, at most one banner shown
- [ ] 6.4 Add the banner strings for the battery state and its button, in every supported language
- [ ] 6.5 Add the vendor auto-start table matched on `Build.MANUFACTURER`, each intent guarded by `resolveActivity`, falling back to the generic system setting when none resolves
- [ ] 6.6 Unit-test the banner precedence and the vendor fallback; add a semantics test for the banner on Home
- [ ] 6.7 Run the `pillsner-ui-review` skill over the banner changes before calling this group done

## 7. Watchdog (design D3)

- [ ] 7.1 Create the periodic worker: recompute the schedule, compare against the recorded armed set, re-arm what is missing, then run a normal wake
- [ ] 7.2 Return immediately when no active medicine produces doses
- [ ] 7.3 Enqueue it as unique periodic work with `ExistingPeriodicWorkPolicy.KEEP` from `PillsnerApplication.onCreate` and from the boot receiver
- [ ] 7.4 Test: alarms intact does nothing; a dropped alarm is re-armed and the due dose posted; no alarms at all are re-armed from scratch; no medicines ends immediately
- [ ] 7.5 Confirm no medicine name or amount is logged anywhere in the worker or the scheduler, and dose ids only at debug level

## 8. Wiring

- [ ] 8.1 Register the new scheduler, service entry point, watchdog and battery-optimisation source in `AppContainer`, keeping construction cheap and side-effect free
- [ ] 8.2 Confirm `PillsnerApplication.onCreate` still starts quickly: the wake goes to the service, and the two `runBlocking` DataStore reads are not joined by a third

## 9. Verification

- [ ] 9.1 Run the unit test task from `src` and report any failure verbatim
- [ ] 9.2 Run the lint task from `src` and clear anything it raises
- [ ] 9.3 Run the instrumented tests: alarm scheduling, notification posting, database migration, boot and unlock
- [ ] 9.3a Re-run the tests `reminder-delivery-after-reboot` added and confirm none regressed: record-when-posted, retry on timeout, locked wake, locked boot re-arm
- [ ] 9.4 Manual test, documented in the change: schedule a dose a few minutes out, background the app, make a phone call across the due moment, confirm the reminder arrives within the minute
- [ ] 9.5 Manual test: force-stop nothing, leave the device idle overnight with a morning dose, confirm the reminder arrives
- [ ] 9.6 Manual test: reboot across a due dose without unlocking, confirm the reminder arrives
- [ ] 9.7 Manual test: deny the battery exemption, confirm the banner appears and its button opens the right system screen

## 10. Documentation

- [ ] 10.1 Update the README: the status-bar alarm icon a scheduled medicine now causes, the battery-optimisation request and why the app asks for it, and the new permissions
- [ ] 10.2 Record the Play Console declaration text for `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, citing the exact-alarm core function
- [ ] 10.3 Confirm no network permission, third-party SDK or telemetry was added
